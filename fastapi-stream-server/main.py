import os
from typing import Any

import httpx
from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response, StreamingResponse


DEFAULT_SPRING_BOOT_BASE_URL = "http://34.50.7.78:8080"
SPRING_BOOT_BASE_URL = os.getenv(
    "SPRING_BOOT_BASE_URL",
    DEFAULT_SPRING_BOOT_BASE_URL,
).rstrip("/")

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
            "GET /video/{device_id}",
            "GET /video/by-mac?macAddress=...",
        ],
    }


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/register")
async def register(request: Request) -> Response:
    return await _proxy_post_to_spring("/api/raspberrypis/register", request)


@app.post("/device/{cage_id}/sensor")
async def receive_sensor(cage_id: int, request: Request) -> dict[str, Any]:
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
            response = await client.get(url, params=params)
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
    headers = {}

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
