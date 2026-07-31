# 라즈베리파이 IP 카메라 자동 송출

라즈베리파이가 같은 LAN에 있는 Tapo 카메라의 `stream2`를 읽어 GCP
MediaMTX로 전송합니다. 카메라 RTSP 계정은 라즈베리파이에만 저장하며
Spring Boot로 전달하거나 Git에 커밋하지 않습니다.

## 전제 조건

- IP 카메라와 라즈베리파이가 같은 Wi-Fi 또는 LAN에 연결되어 있어야 합니다.
- 카메라 주소는 공인 IP가 아닌 `192.168.x.x` 같은 내부 IP를 사용합니다.
- 라즈베리파이에서 `ffmpeg` 명령을 실행할 수 있어야 합니다.
- GCP MediaMTX의 `8554/tcp` 포트에 연결할 수 있어야 합니다.

FFmpeg가 없다면 다음 명령으로 설치합니다.

```bash
sudo apt update
sudo apt install -y ffmpeg
```

## 자동 실행 서비스 설치

```bash
cd raspberry-pi
chmod +x install_camera_service.sh stream_camera.sh
./install_camera_service.sh
```

설정파일을 엽니다.

```bash
sudo nano /etc/peztz/camera-stream.env
```

기존 수동 송출 테스트에서 성공한 실제 값으로 변경합니다.

```text
CAMERA_RTSP_URL=rtsp://RTSP계정:RTSP비밀번호@카메라내부IP:554/stream2
MEDIAMTX_PUBLISH_URL=rtsp://송출계정:송출비밀번호@GCP공인IP:8554/cage-a1
```

계정이나 비밀번호에 URL 예약 문자가 있다면 URL 인코딩해야 합니다.
`MEDIAMTX_PUBLISH_URL`의 경로와 GCP FastAPI의 `MEDIAMTX_STREAM_PATH`는
동일해야 합니다. 현재 검증된 테스트 경로는 `cage-a1`입니다.

서비스를 자동 실행으로 등록하고 바로 시작합니다.

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now peztz-camera
```

## 상태 및 로그 확인

```bash
systemctl status peztz-camera --no-pager
journalctl -u peztz-camera -n 50 --no-pager
```

실시간 로그를 보려면 다음 명령을 사용합니다.

```bash
journalctl -u peztz-camera -f
```

로그 화면에서 `Ctrl+C`를 눌러도 로그 보기만 종료되며 송출 서비스는 계속
실행됩니다.

## 자동 복구 확인

라즈베리파이를 재부팅한 뒤 서비스가 자동으로 실행되는지 확인합니다.

```bash
sudo reboot
```

재접속 후:

```bash
systemctl status peztz-camera --no-pager
```

카메라, Wi-Fi 또는 네트워크 연결이 끊겨 FFmpeg가 종료되면 서비스가 5초 후
자동으로 다시 실행합니다.

## 수동 제어

```bash
sudo systemctl stop peztz-camera
sudo systemctl start peztz-camera
sudo systemctl restart peztz-camera
```

오류 분석이 필요하면 다음 로그를 확인합니다.

```bash
journalctl -u peztz-camera -n 100 --no-pager
```
