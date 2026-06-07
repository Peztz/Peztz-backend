# Peztz Frontend API Guide

Spring Boot 백엔드 API는 Swagger UI에서 직접 확인하고 테스트할 수 있습니다.

## Swagger 접속 주소

| 환경 | Swagger UI | OpenAPI JSON |
| --- | --- | --- |
| Local | `http://localhost:8080/swagger-ui/index.html` | `http://localhost:8080/v3/api-docs` |
| GCP | `http://34.50.7.78:8080/swagger-ui/index.html` | `http://34.50.7.78:8080/v3/api-docs` |

GCP API Base URL:

```text
http://34.50.7.78:8080
```

## 프론트에서 우선 연결할 API

1. `GET /api/raspberrypis`
2. `GET /api/raspberrypis/{deviceId}/stream-url`
3. `GET /api/raspberrypis/stream-url?macAddress=...`

참고:

- `POST /api/raspberrypis/register`는 주로 라즈베리파이 Python 코드가 호출하는 등록/상태 갱신 API입니다.
- 프론트는 보통 목록 조회 후 선택한 장치의 `streamUrl`을 조회하는 흐름을 사용합니다.
- `streamUrl`은 프론트에서 직접 조합하지 말고 백엔드 응답값을 사용하세요.

## 기본 연결 흐름

```text
1. GET /api/raspberrypis 로 등록된 라즈베리파이 목록 조회
2. 사용자가 특정 라즈베리파이 또는 케이지 선택
3. 선택한 deviceId로 GET /api/raspberrypis/{deviceId}/stream-url 호출
4. 응답의 streamUrl 값을 <img src={streamUrl}>에 넣어 영상 표시
```

## 스트리밍 제한사항

현재 `streamUrl` 예시:

```text
http://192.168.150.142:8001/video_feed
```

프론트 사용 예시:

```html
<img src="http://192.168.150.142:8001/video_feed" alt="Raspberry Pi stream" />
```

제한사항:

- 현재 `streamUrl`은 라즈베리파이 내부 IP 기반입니다.
- 같은 Wi-Fi 또는 같은 내부망에서는 영상 확인이 가능합니다.
- 외부 인터넷 환경에서는 이 `streamUrl`만으로는 영상이 보이지 않을 수 있습니다.
- 외부 영상 스트리밍은 추후 FastAPI 또는 별도 중계 서버가 필요합니다.
- 현재 Spring Boot API는 `streamUrl`을 생성해서 내려주는 역할까지만 담당합니다.

## JavaScript fetch 예시

```javascript
const API_BASE_URL = "http://34.50.7.78:8080";

async function loadRaspberryPis() {
  const response = await fetch(`${API_BASE_URL}/api/raspberrypis`);

  if (!response.ok) {
    throw new Error("라즈베리파이 목록 조회 실패");
  }

  return await response.json();
}

async function loadStreamUrlByDeviceId(deviceId) {
  const response = await fetch(
    `${API_BASE_URL}/api/raspberrypis/${deviceId}/stream-url`
  );

  if (!response.ok) {
    throw new Error("streamUrl 조회 실패");
  }

  return await response.json();
}

async function loadStreamUrlByMacAddress(macAddress) {
  const encodedMacAddress = encodeURIComponent(macAddress);
  const response = await fetch(
    `${API_BASE_URL}/api/raspberrypis/stream-url?macAddress=${encodedMacAddress}`
  );

  if (!response.ok) {
    throw new Error("MAC 주소 기반 streamUrl 조회 실패");
  }

  return await response.json();
}

async function showRaspberryPiStream(deviceId, imgElement) {
  try {
    const data = await loadStreamUrlByDeviceId(deviceId);
    imgElement.src = data.streamUrl;
  } catch (error) {
    console.error(error);
    imgElement.removeAttribute("src");
    alert("영상 스트림 정보를 불러오지 못했습니다.");
  }
}
```

## curl 테스트

```bash
curl http://34.50.7.78:8080/api/raspberrypis
```

```bash
curl http://34.50.7.78:8080/api/raspberrypis/{deviceId}/stream-url
```

```bash
curl "http://34.50.7.78:8080/api/raspberrypis/stream-url?macAddress=88:A2:9E:3D:02:BD"
```

## FastAPI Video Proxy

Frontend clients should not use the Spring Boot `streamUrl` directly when
displaying live video. The `streamUrl` can contain a Raspberry Pi Tailscale IP,
which should stay behind the GCP FastAPI proxy.

Use this final video URL:

```text
http://34.50.7.78:8000/video/{deviceId}
```

Device ID example:

```javascript
const deviceId = "7bf2b0d2-dd67-4002-929a-d4505f6af890";
const videoUrl = `http://34.50.7.78:8000/video/${deviceId}`;
document.getElementById("cameraStream").src = videoUrl;
```

```html
<img id="cameraStream" alt="라즈베리파이 실시간 영상" />
```

MAC address proxy URL:

```text
http://34.50.7.78:8000/video/by-mac?macAddress=88%3AA2%3A9E%3A3D%3A02%3ABD
```

Spring Boot remains the source of Raspberry Pi metadata and `streamUrl` lookup,
but the browser should render the MJPEG stream through FastAPI:

```html
<img src="http://34.50.7.78:8000/video/7bf2b0d2-dd67-4002-929a-d4505f6af890" />
```
