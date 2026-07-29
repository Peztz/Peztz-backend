# Peztz FastAPI Video Proxy

FastAPI server for proxying Raspberry Pi MJPEG streams through the GCP server.
The frontend should request this server instead of using the Raspberry Pi
Tailscale IP directly.

## APIs

- `GET /`
- `GET /health`
- `POST /register`
- `POST /device/{cage_id}/sensor`
- `POST /device/events`
- `GET /video/{device_id}`
- `GET /video/by-mac?macAddress=88:A2:9E:3D:02:BD`
- `GET /internal/cameras/{camera_id}/status`
- `POST /internal/cameras/{camera_id}/live/start`
- `POST /internal/cameras/{camera_id}/live/stop`

`/register` proxies Raspberry Pi registration requests to Spring Boot:

```text
POST {SPRING_BOOT_BASE_URL}/api/raspberrypis/register
```

`/device/{cage_id}/sensor` is kept for compatibility with the existing GCP
FastAPI service.

`/video/{device_id}` calls Spring Boot first:

```text
GET {SPRING_BOOT_BASE_URL}/api/raspberrypis/{device_id}/stream-url
```

`/video/by-mac` calls Spring Boot first:

```text
GET {SPRING_BOOT_BASE_URL}/api/raspberrypis/stream-url?macAddress=...
```

The FastAPI server then connects to the returned `streamUrl` and proxies the
MJPEG stream with this response content type:

```text
multipart/x-mixed-replace; boundary=frame
```

## Install

```bash
cd fastapi-stream-server
pip install -r requirements.txt
```

Using a virtual environment is recommended:

```bash
cd fastapi-stream-server
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

On Windows PowerShell:

```powershell
cd fastapi-stream-server
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## Configuration

Spring Boot base URL is read from this environment variable:

```text
SPRING_BOOT_BASE_URL
```

Default value:

```text
http://34.50.7.78:8080
```

Production deployments must also configure these secrets. They have no source
code defaults:

```text
SPRING_INTERNAL_API_KEY
DEVICE_API_KEY
FASTAPI_INTERNAL_API_KEY
```

`MEDIAMTX_PLAYBACK_BASE_URL` is reserved for the next live-stream phase. The
current start/stop endpoints only return `NOT_IMPLEMENTED`/`IDLE` state and do
not start RTSP, ffmpeg, or MediaMTX.

Raspberry Pi registration and `/device/events` require `X-Device-Api-Key`.
Spring-to-FastAPI camera control requires `X-Internal-Api-Key`. FastAPI sends
`SPRING_INTERNAL_API_KEY` to Spring internal APIs.

Linux/macOS:

```bash
export SPRING_BOOT_BASE_URL=http://34.50.7.78:8080
```

Windows PowerShell:

```powershell
$env:SPRING_BOOT_BASE_URL = "http://34.50.7.78:8080"
```

## Run

```bash
cd fastapi-stream-server
uvicorn main:app --host 0.0.0.0 --port 8000
```

Alternative:

```bash
python -m uvicorn main:app --host 0.0.0.0 --port 8000
```

## Test

Health check does not require the Raspberry Pi camera stream to be running:

```bash
curl http://localhost:8000/health
```

Video proxy tests require `camera_stream.py` or the Raspberry Pi MJPEG server
to be running:

```bash
curl -I --max-time 5 http://localhost:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890
```

```bash
curl -I --max-time 5 "http://localhost:8000/video/by-mac?macAddress=88:A2:9E:3D:02:BD"
```

After GCP deployment:

```bash
curl http://34.50.7.78:8000/health
```

```bash
curl -I --max-time 5 http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890
```

## Frontend Example

Use the FastAPI proxy URL:

```text
http://34.50.7.78:8000/video/{deviceId}
```

Example:

```html
<img src="http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890" />
```

Do not put the Spring Boot `streamUrl` directly into the frontend image source
for remote users. That URL can contain a Raspberry Pi Tailscale IP.

## GCP systemd Example

The existing GCP service runs from this directory:

```text
/home/junghyun47483/peztz-api
```

Deploy by replacing the FastAPI files in that directory, especially:

```text
/home/junghyun47483/peztz-api/main.py
/home/junghyun47483/peztz-api/requirements.txt
```

The existing systemd service name is:

```text
peztz-api.service
```

Service file example:

```ini
[Unit]
Description=Peztz FastAPI Video Proxy
After=network.target

[Service]
User=junghyun47483
WorkingDirectory=/home/junghyun47483/peztz-api
Environment=SPRING_BOOT_BASE_URL=http://34.50.7.78:8080
ExecStart=/usr/bin/python3 -m uvicorn main:app --host 0.0.0.0 --port 8000
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
sudo systemctl daemon-reload
sudo systemctl restart peztz-api.service
sudo systemctl status peztz-api.service
```

Check logs:

```bash
journalctl -u peztz-api.service -f
```

If you use a virtual environment at
`/home/junghyun47483/peztz-api/.venv`, set `ExecStart` to:

```ini
ExecStart=/home/junghyun47483/peztz-api/.venv/bin/python -m uvicorn main:app --host 0.0.0.0 --port 8000
```

Install/update dependencies on GCP:

```bash
cd /home/junghyun47483/peztz-api
pip install -r requirements.txt
```

Test on GCP:

```bash
curl http://34.50.7.78:8000/
curl http://34.50.7.78:8000/health
curl -I --max-time 5 http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890
```
