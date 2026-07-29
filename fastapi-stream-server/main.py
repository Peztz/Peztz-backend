import asyncio
import hmac
import json
import logging
import os
from typing import Any

import httpx
from fastapi import FastAPI, Header, HTTPException, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response, StreamingResponse
# 여기서 부터는 LLM 코드 
import asyncpg
from pydantic import BaseModel
from google import genai
from google.genai import types
from dotenv import load_dotenv

# .env 파일 로드 (GEMINI_API_KEY 환경변수를 불러옵니다)
load_dotenv()

logger = logging.getLogger(__name__)


DEFAULT_SPRING_BOOT_BASE_URL = "http://34.50.7.78:8080"
SPRING_BOOT_BASE_URL = os.getenv(
    "SPRING_BOOT_BASE_URL",
    DEFAULT_SPRING_BOOT_BASE_URL,
).rstrip("/")
SPRING_INTERNAL_API_KEY = os.getenv("SPRING_INTERNAL_API_KEY", "")
DEVICE_API_KEY = os.getenv("DEVICE_API_KEY", "")
FASTAPI_INTERNAL_API_KEY = os.getenv("FASTAPI_INTERNAL_API_KEY", "")
MEDIAMTX_PLAYBACK_BASE_URL = os.getenv("MEDIAMTX_PLAYBACK_BASE_URL", "").rstrip("/")

MJPEG_MEDIA_TYPE = "multipart/x-mixed-replace; boundary=frame"

app = FastAPI(title="Peztz FastAPI Video Proxy")

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:3000",
        "http://localhost:5173",
        "http://34.50.7.78:8080",
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
            "GET /video/{device_id}",
            "GET /video/by-mac?macAddress=...",
            "GET /internal/cameras/{camera_id}/status",
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


@app.head("/video/by-mac")
async def video_by_mac_head(
    macAddress: str = Query(..., min_length=1),
) -> Response:
    stream_url = await _get_stream_url_from_spring(
        "/api/raspberrypis/stream-url",
        params={"macAddress": macAddress},
    )
    return await _check_mjpeg_stream(stream_url)


@app.get("/video/by-mac")
async def video_by_mac(
    macAddress: str = Query(..., min_length=1),
) -> StreamingResponse:
    stream_url = await _get_stream_url_from_spring(
        "/api/raspberrypis/stream-url",
        params={"macAddress": macAddress},
    )
    return await _proxy_mjpeg_stream(stream_url)


@app.head("/video/{device_id}")
async def video_by_device_id_head(device_id: str) -> Response:
    stream_url = await _get_stream_url_from_spring(
        f"/api/raspberrypis/{device_id}/stream-url",
    )
    return await _check_mjpeg_stream(stream_url)


@app.get("/video/{device_id}")
async def video_by_device_id(device_id: str) -> StreamingResponse:
    stream_url = await _get_stream_url_from_spring(
        f"/api/raspberrypis/{device_id}/stream-url",
    )
    return await _proxy_mjpeg_stream(stream_url)


async def _get_stream_url_from_spring(
    path: str,
    params: dict[str, str] | None = None,
) -> str:
    url = f"{SPRING_BOOT_BASE_URL}{path}"
    timeout = httpx.Timeout(10.0, connect=5.0)

    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            response = await client.get(
                url,
                params=params,
                headers=_spring_internal_headers(),
            )
    except httpx.TimeoutException as exc:
        raise HTTPException(
            status_code=502,
            detail="Spring Boot streamUrl API timeout",
        ) from exc
    except httpx.RequestError as exc:
        raise HTTPException(
            status_code=502,
            detail="Spring Boot streamUrl API request failed",
        ) from exc

    if response.status_code == 404:
        raise HTTPException(status_code=404, detail="Raspberry Pi device not found")

    if response.status_code >= 400:
        raise HTTPException(
            status_code=502,
            detail="Spring Boot streamUrl API returned an error",
        )

    try:
        data: dict[str, Any] = response.json()
    except ValueError as exc:
        raise HTTPException(
            status_code=502,
            detail="Spring Boot streamUrl API returned invalid JSON",
        ) from exc

    stream_url = data.get("streamUrl")
    if not isinstance(stream_url, str) or not stream_url.strip():
        raise HTTPException(status_code=400, detail="streamUrl is missing")

    return stream_url.strip()


async def _proxy_post_to_spring(path: str, request: Request) -> Response:
    url = f"{SPRING_BOOT_BASE_URL}{path}"
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


async def _proxy_mjpeg_stream(stream_url: str) -> StreamingResponse:
    client, stream_context, upstream_response = await _open_mjpeg_upstream(stream_url)

    async def stream_chunks():
        try:
            async for chunk in upstream_response.aiter_bytes():
                if chunk:
                    yield chunk
        finally:
            await stream_context.__aexit__(None, None, None)
            await client.aclose()

    return StreamingResponse(
        stream_chunks(),
        media_type=MJPEG_MEDIA_TYPE,
        headers=_stream_headers(),
    )


async def _check_mjpeg_stream(stream_url: str) -> Response:
    client, stream_context, _ = await _open_mjpeg_upstream(stream_url)
    await stream_context.__aexit__(None, None, None)
    await client.aclose()

    return Response(
        media_type=MJPEG_MEDIA_TYPE,
        headers=_stream_headers(),
    )


async def _open_mjpeg_upstream(stream_url: str):
    timeout = httpx.Timeout(connect=5.0, read=None, write=5.0, pool=5.0)
    client = httpx.AsyncClient(timeout=timeout, follow_redirects=True)
    stream_context = client.stream("GET", stream_url)

    try:
        upstream_response = await stream_context.__aenter__()
    except httpx.TimeoutException as exc:
        await client.aclose()
        raise HTTPException(
            status_code=502,
            detail="Raspberry Pi stream server timeout",
        ) from exc
    except httpx.RequestError as exc:
        await client.aclose()
        raise HTTPException(
            status_code=502,
            detail="Raspberry Pi stream server request failed",
        ) from exc

    if upstream_response.status_code >= 400:
        await stream_context.__aexit__(None, None, None)
        await client.aclose()
        raise HTTPException(
            status_code=502,
            detail="Raspberry Pi stream server returned an error",
        )

    return client, stream_context, upstream_response


def _stream_headers() -> dict[str, str]:
    return {
        "Cache-Control": "no-cache",
        "Pragma": "no-cache",
        "X-Accel-Buffering": "no",
    }


def _spring_internal_headers() -> dict[str, str]:
    if not SPRING_INTERNAL_API_KEY:
        raise HTTPException(
            status_code=503,
            detail="Spring internal API key is not configured",
        )
    return {"X-Internal-Api-Key": SPRING_INTERNAL_API_KEY}


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
    petId: str
    cameraId: str
    eventType: str
    confidence: float
    occurredAt: str
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
    url = f"{SPRING_BOOT_BASE_URL}/api/internal/pet-events"
    timeout = httpx.Timeout(10.0, connect=5.0)
    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            response = await client.post(
                url,
                json=event.model_dump(),
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


camera_runtime_statuses: dict[str, str] = {}


def _camera_runtime_response(camera_id: str) -> dict[str, Any]:
    return {
        "cameraId": camera_id,
        "status": camera_runtime_statuses.get(camera_id, "IDLE"),
        "playbackUrl": None,
        "message": "Camera control skeleton only; no RTSP or MediaMTX process was started",
    }


@app.get("/internal/cameras/{camera_id}/status")
async def camera_runtime_status(
    camera_id: str,
    x_internal_api_key: str | None = Header(default=None, alias="X-Internal-Api-Key"),
) -> dict[str, Any]:
    _require_api_key(x_internal_api_key, FASTAPI_INTERNAL_API_KEY, "FastAPI internal API key")
    return _camera_runtime_response(camera_id)


@app.post("/internal/cameras/{camera_id}/live/start")
async def start_camera_live_skeleton(
    camera_id: str,
    x_internal_api_key: str | None = Header(default=None, alias="X-Internal-Api-Key"),
) -> dict[str, Any]:
    _require_api_key(x_internal_api_key, FASTAPI_INTERNAL_API_KEY, "FastAPI internal API key")
    camera_runtime_statuses[camera_id] = "NOT_IMPLEMENTED"
    return _camera_runtime_response(camera_id)


@app.post("/internal/cameras/{camera_id}/live/stop")
async def stop_camera_live_skeleton(
    camera_id: str,
    x_internal_api_key: str | None = Header(default=None, alias="X-Internal-Api-Key"),
) -> dict[str, Any]:
    _require_api_key(x_internal_api_key, FASTAPI_INTERNAL_API_KEY, "FastAPI internal API key")
    camera_runtime_statuses[camera_id] = "IDLE"
    return _camera_runtime_response(camera_id)

# 여기서는 llm 
DB_USER = os.getenv("DB_USER")
DB_PASSWORD = os.getenv("DB_PASSWORD")
DB_HOST = os.getenv("DB_HOST")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME")

class ReportRequest(BaseModel):
    cage_id: str  
    pet_name: str


def _generate_gemini_report(user_message: str, system_instruction: str):
    return genai.Client().models.generate_content(
        model="gemini-2.5-flash",
        contents=user_message,
        config=types.GenerateContentConfig(
            system_instruction=system_instruction,
            temperature=0.6,
        ),
    )

@app.post("/api/report/generate")
async def generate_pet_report(request: ReportRequest):
    conn = None
    try:
        if not all((DB_USER, DB_PASSWORD, DB_HOST, DB_NAME)):
            raise RuntimeError("Database configuration is incomplete")

        # 1. PostgreSQL DB 연결
        conn = await asyncpg.connect(
            user=DB_USER, password=DB_PASSWORD,
            database=DB_NAME, host=DB_HOST, port=DB_PORT
        )
        
        # 2. 쿼리문 실행 (현재 케이지의 최근 20개 로그 추출)
        query = """
            SELECT l.log_type, l.data, l.created_at
            FROM public.pet_logs l
            JOIN public.access_session s ON l.session_id = s.session_id
            WHERE s.cage_id = $1::uuid
            ORDER BY l.created_at DESC
            LIMIT 20
        """
        rows = await conn.fetch(query, request.cage_id)
        
        # 3. 데이터 가공
        behavior_logs = []
        for row in rows:
            log_type = row['log_type']
            log_data = row['data']
            created_at = row['created_at'].strftime('%H:%M:%S') if row['created_at'] else ""
            
            if isinstance(log_data, dict):
                log_data_str = json.dumps(log_data, ensure_ascii=False)
            else:
                log_data_str = str(log_data)
                
            behavior_logs.append(f"[{created_at} / {log_type}] {log_data_str}")
        
        if not behavior_logs:
            behavior_logs = ["현재 세션에 누적된 Vision AI 행동 로그가 존재하지 않습니다. 반려동물이 매우 평온하고 안정적인 상태입니다."]

        # 4. 프롬프트 세팅
        system_instruction = (
            "당신은 실시간 모니터링 시스템과 Vision AI(YOLO) 시계열 로그를 기반으로 반려동물의 행동을 분석하는 전문 수의사 AI입니다. "
            "제공된 펫 로그를 꼼꼼하게 분석하여 보호자가 직관적으로 이해할 수 있는 '일일 건강 리포트'를 마크다운 서식으로 작성해 주세요."
        )
        
        user_message = f"""
        분석 대상 반려동물 이름: {request.pet_name}
        
        [데이터베이스 추출 실시간 Vision AI 행동 로그]
        {chr(10).join(behavior_logs)}
        
        위 시계열 로그 데이터를 기반으로 아래 레이아웃에 맞춰 친절한 한국어로 리포트를 출력해줘:
        
        ## 🐾 오늘의 요약
        - 오늘 하루 아이의 전반적인 상태를 직관적인 문장으로 요약해 주세요.
        
        ## 📊 Vision AI 행동 분석
        - 로그 내용을 인용하여 상세히 분석해 주세요.
        
        ## 🩺 수의사 AI의 행동 가이드
        - 맞춤형 케어 팁을 제시해 주세요.
        """

        # 5. Gemini 호출
        response = await asyncio.to_thread(
            _generate_gemini_report,
            user_message,
            system_instruction,
        )
        
        ai_report_text = response.text

        return {
            "status": "success",
            "cageId": request.cage_id,
            "petName": request.pet_name,
            "report": ai_report_text
        }

    except Exception as exc:
        logger.exception("Pet report generation failed")
        raise HTTPException(status_code=500, detail="Pet report generation failed") from exc
        
    finally:
        if conn:
            await conn.close()
