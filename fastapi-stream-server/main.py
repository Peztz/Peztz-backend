import asyncio
import hmac
import logging
import os
from datetime import date
from enum import Enum
from typing import Any
from urllib.parse import quote

import httpx
from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response
from openai import OpenAI
from pydantic import BaseModel, Field
from dotenv import load_dotenv

from report_prompt import build_daily_report_messages

# Local development reads secrets from a Git-ignored .env file.
load_dotenv()

logger = logging.getLogger(__name__)


SPRING_BOOT_BASE_URL = os.getenv("SPRING_BOOT_BASE_URL", "").rstrip("/")
SPRING_INTERNAL_API_KEY = os.getenv("SPRING_INTERNAL_API_KEY", "")
DEVICE_API_KEY = os.getenv("DEVICE_API_KEY", "")
FASTAPI_INTERNAL_API_KEY = os.getenv("FASTAPI_INTERNAL_API_KEY", "")
MEDIAMTX_PLAYBACK_BASE_URL = os.getenv("MEDIAMTX_PLAYBACK_BASE_URL", "").rstrip("/")
MEDIAMTX_API_BASE_URL = os.getenv("MEDIAMTX_API_BASE_URL", "http://127.0.0.1:9997").rstrip("/")
MEDIAMTX_STREAM_PATH = os.getenv("MEDIAMTX_STREAM_PATH", "").strip("/")
MEDIAMTX_STREAM_PATH_TEMPLATE = os.getenv(
    "MEDIAMTX_STREAM_PATH_TEMPLATE",
    "camera-{camera_id}",
).strip("/")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-5-mini")
OPENAI_TIMEOUT_SECONDS = float(os.getenv("OPENAI_TIMEOUT_SECONDS", "90"))
OPENAI_MAX_RETRIES = int(os.getenv("OPENAI_MAX_RETRIES", "1"))

app = FastAPI(title="Peztz FastAPI Device Integration")

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:3000",
        "http://localhost:5173",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
async def root() -> dict[str, Any]:
    return {
        "status": "ok",
        "service": "Peztz FastAPI server",
        "apis": [
            "GET /",
            "GET /health",
            "POST /register",
            "POST /device/{cage_id}/sensor",
            "POST /device/events",
            "GET /internal/cameras/{camera_id}/status",
            "POST /internal/reports/daily/generate",
        ],
    }


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/register")
async def register(
    request: Request,
    x_device_api_key: str | None = Header(default=None, alias="X-Device-Api-Key"),
) -> Response:
    _require_api_key(x_device_api_key, DEVICE_API_KEY, "Device API key")
    return await _proxy_post_to_spring("/api/raspberrypis/register", request)


@app.post("/device/{cage_id}/sensor")
async def receive_sensor(
    cage_id: int,
    request: Request,
    x_device_api_key: str | None = Header(default=None, alias="X-Device-Api-Key"),
) -> dict[str, Any]:
    _require_api_key(x_device_api_key, DEVICE_API_KEY, "Device API key")
    payload = await _read_json_payload(request)
    return {
        "status": "ok",
        "cageId": cage_id,
        "received": payload,
    }


async def _proxy_post_to_spring(path: str, request: Request) -> Response:
    url = _spring_boot_url(path)
    body = await request.body()
    headers = _spring_internal_headers()

    content_type = request.headers.get("content-type")
    if content_type:
        headers["content-type"] = content_type

    timeout = httpx.Timeout(10.0, connect=5.0)

    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            response = await client.post(url, content=body, headers=headers)
    except httpx.TimeoutException as exc:
        raise HTTPException(
            status_code=502,
            detail="Spring Boot API timeout",
        ) from exc
    except httpx.RequestError as exc:
        raise HTTPException(
            status_code=502,
            detail="Spring Boot API request failed",
        ) from exc

    return Response(
        content=response.content,
        status_code=response.status_code,
        media_type=response.headers.get("content-type", "application/json"),
    )


async def _read_json_payload(request: Request) -> Any:
    try:
        return await request.json()
    except ValueError:
        body = await request.body()
        return body.decode("utf-8", errors="replace") if body else None


def _spring_internal_headers() -> dict[str, str]:
    if not SPRING_INTERNAL_API_KEY:
        raise HTTPException(
            status_code=503,
            detail="Spring internal API key is not configured",
        )
    return {"X-Internal-Api-Key": SPRING_INTERNAL_API_KEY}


def _spring_boot_url(path: str) -> str:
    if not SPRING_BOOT_BASE_URL:
        raise HTTPException(status_code=503, detail="Spring Boot base URL is not configured")
    return f"{SPRING_BOOT_BASE_URL}{path}"


def _require_api_key(
    provided_api_key: str | None,
    configured_api_key: str,
    key_name: str,
) -> None:
    if not configured_api_key:
        raise HTTPException(status_code=503, detail=f"{key_name} is not configured")
    if not provided_api_key or not hmac.compare_digest(provided_api_key, configured_api_key):
        raise HTTPException(status_code=401, detail=f"Invalid {key_name.lower()}")


class PetEventResult(BaseModel):
    externalEventId: str
    cameraId: str
    eventType: str
    confidence: float
    occurredAt: str
    petId: str | None = None
    eventEndedAt: str | None = None
    eventDurationSeconds: int | None = None
    clipStartAt: str | None = None
    clipEndAt: str | None = None
    clipDurationSeconds: int | None = None
    videoUrl: str | None = None
    thumbnailUrl: str | None = None
    metadata: dict[str, Any] | None = None


@app.post("/device/events")
async def forward_pet_event(
    event: PetEventResult,
    x_device_api_key: str | None = Header(default=None, alias="X-Device-Api-Key"),
) -> Response:
    _require_api_key(x_device_api_key, DEVICE_API_KEY, "Device API key")
    url = _spring_boot_url("/api/internal/pet-events")
    timeout = httpx.Timeout(10.0, connect=5.0)
    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            response = await client.post(
                url,
                json=event.model_dump(exclude_none=True),
                headers=_spring_internal_headers(),
            )
    except httpx.TimeoutException as exc:
        raise HTTPException(status_code=502, detail="Spring event API timeout") from exc
    except httpx.RequestError as exc:
        raise HTTPException(status_code=502, detail="Spring event API request failed") from exc

    return Response(
        content=response.content,
        status_code=response.status_code,
        media_type=response.headers.get("content-type", "application/json"),
    )


def _camera_stream_path(camera_id: str) -> str:
    if MEDIAMTX_STREAM_PATH:
        return MEDIAMTX_STREAM_PATH
    try:
        stream_path = MEDIAMTX_STREAM_PATH_TEMPLATE.format(camera_id=camera_id).strip("/")
    except (KeyError, ValueError) as exc:
        raise HTTPException(
            status_code=503,
            detail="MediaMTX stream path template is invalid",
        ) from exc
    if not stream_path:
        raise HTTPException(status_code=503, detail="MediaMTX stream path is not configured")
    return stream_path


async def _mediamtx_path_is_online(stream_path: str) -> bool:
    if not MEDIAMTX_API_BASE_URL:
        raise HTTPException(status_code=503, detail="MediaMTX API base URL is not configured")

    path_url = f"{MEDIAMTX_API_BASE_URL}/v3/paths/get/{quote(stream_path, safe='')}"
    timeout = httpx.Timeout(5.0, connect=2.0)
    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            response = await client.get(path_url)
    except httpx.TimeoutException as exc:
        raise HTTPException(status_code=502, detail="MediaMTX API timeout") from exc
    except httpx.RequestError as exc:
        raise HTTPException(status_code=502, detail="MediaMTX API request failed") from exc

    if response.status_code == 404:
        return False
    if response.status_code >= 400:
        raise HTTPException(
            status_code=502,
            detail=f"MediaMTX API returned status {response.status_code}",
        )

    try:
        path_status = response.json()
    except ValueError as exc:
        raise HTTPException(status_code=502, detail="MediaMTX API returned invalid JSON") from exc
    if not isinstance(path_status, dict):
        raise HTTPException(status_code=502, detail="MediaMTX API returned an invalid response")

    return path_status.get("ready") is True


async def _camera_runtime_response(camera_id: str) -> dict[str, Any]:
    stream_path = _camera_stream_path(camera_id)
    is_online = await _mediamtx_path_is_online(stream_path)
    playback_url = None
    if is_online and MEDIAMTX_PLAYBACK_BASE_URL:
        playback_url = f"{MEDIAMTX_PLAYBACK_BASE_URL}/{quote(stream_path, safe='')}/"

    if not is_online:
        message = "Raspberry Pi is not publishing this camera stream"
    elif playback_url is None:
        message = "Camera stream is online, but the playback base URL is not configured"
    else:
        message = "Camera stream is online"

    return {
        "cameraId": camera_id,
        "status": "ONLINE" if is_online else "IDLE",
        "playbackUrl": playback_url,
        "message": message,
    }


@app.get("/internal/cameras/{camera_id}/status")
async def camera_runtime_status(
    camera_id: str,
    x_internal_api_key: str | None = Header(default=None, alias="X-Internal-Api-Key"),
) -> dict[str, Any]:
    _require_api_key(x_internal_api_key, FASTAPI_INTERNAL_API_KEY, "FastAPI internal API key")
    return await _camera_runtime_response(camera_id)


@app.post("/internal/cameras/{camera_id}/live/start")
async def start_camera_live_stream(
    camera_id: str,
    x_internal_api_key: str | None = Header(default=None, alias="X-Internal-Api-Key"),
) -> dict[str, Any]:
    _require_api_key(x_internal_api_key, FASTAPI_INTERNAL_API_KEY, "FastAPI internal API key")
    raise HTTPException(
        status_code=409,
        detail="Live stream is managed by the Raspberry Pi systemd service",
    )


@app.post("/internal/cameras/{camera_id}/live/stop")
async def stop_camera_live_stream(
    camera_id: str,
    x_internal_api_key: str | None = Header(default=None, alias="X-Internal-Api-Key"),
) -> dict[str, Any]:
    _require_api_key(x_internal_api_key, FASTAPI_INTERNAL_API_KEY, "FastAPI internal API key")
    raise HTTPException(
        status_code=409,
        detail="Live stream is managed by the Raspberry Pi systemd service",
    )

class RiskLevel(str, Enum):
    NORMAL = "NORMAL"
    ATTENTION = "ATTENTION"
    URGENT = "URGENT"


class ReportStatistics(BaseModel):
    totalLogCount: int = Field(ge=0)
    sensorLogCount: int = Field(ge=0)
    averageTemperature: float | None = None
    averageHumidity: float | None = None
    doorOpenCount: int = Field(default=0, ge=0)
    lowLightCount: int = Field(default=0, ge=0)


class ReportEvent(BaseModel):
    type: str = Field(min_length=1, max_length=50)
    occurredAt: str
    durationSeconds: int | None = Field(default=None, ge=0)
    message: str | None = Field(default=None, max_length=500)
    temperature: float | None = None
    humidity: float | None = None
    confidence: float | None = Field(default=None, ge=0, le=1)


class DailyReportGenerationRequest(BaseModel):
    reportDate: date
    petName: str = Field(min_length=1, max_length=50)
    breed: str | None = Field(default=None, max_length=50)
    birthDate: date | None = None
    statistics: ReportStatistics
    events: list[ReportEvent] = Field(default_factory=list, max_length=200)


class BehaviorCard(BaseModel):
    title: str
    description: str
    evidence: list[str]


class EnvironmentCard(BaseModel):
    title: str
    description: str


class DailyReportGenerationResponse(BaseModel):
    summary: str
    behaviorCards: list[BehaviorCard]
    environmentCard: EnvironmentCard
    careTips: list[str]
    riskLevel: RiskLevel
    warnings: list[str]
    disclaimer: str


def _generate_openai_daily_report(
    request: DailyReportGenerationRequest,
) -> DailyReportGenerationResponse:
    if not OPENAI_API_KEY:
        raise RuntimeError("OPENAI_API_KEY is not configured")

    client = OpenAI(
        api_key=OPENAI_API_KEY,
        timeout=OPENAI_TIMEOUT_SECONDS,
        max_retries=OPENAI_MAX_RETRIES,
    )
    response = client.responses.parse(
        model=OPENAI_MODEL,
        store=False,
        input=build_daily_report_messages(request.model_dump(mode="json")),
        text_format=DailyReportGenerationResponse,
    )
    if response.output_parsed is None:
        raise RuntimeError("OpenAI returned no structured report")
    return response.output_parsed


@app.post(
    "/internal/reports/daily/generate",
    response_model=DailyReportGenerationResponse,
)
async def generate_daily_report(
    request: DailyReportGenerationRequest,
    x_internal_api_key: str | None = Header(default=None, alias="X-Internal-Api-Key"),
) -> DailyReportGenerationResponse:
    _require_api_key(x_internal_api_key, FASTAPI_INTERNAL_API_KEY, "FastAPI internal API key")
    if not OPENAI_API_KEY:
        raise HTTPException(status_code=503, detail="OpenAI API key is not configured")
    try:
        return await asyncio.to_thread(_generate_openai_daily_report, request)
    except HTTPException:
        raise
    except Exception as exc:
        logger.exception("OpenAI daily report generation failed")
        raise HTTPException(status_code=502, detail="OpenAI report generation failed") from exc
