# Peztz Backend

실시간 모니터링 펫 케어 시스템의 백엔드 저장소입니다.
본 프로젝트는 Spring Boot 기반 중앙 백엔드 서버와 Python/FastAPI 기반 영상·AI 처리 서버를 분리하여 개발합니다.

## 프로젝트 구조

```text
Peztz-backend/
│
├── spring-server/
│   └── Spring Boot 메인 백엔드 서버
│
├── fastapi-stream-server/
│   └── Python/FastAPI 기반 영상 스트리밍 및 AI 분석 서버
│
├── raspberry-pi/
│   └── 라즈베리파이에서 직접 실행하는 기기 등록 및 센서 전송 코드
│
├── docs/
│   └── API 명세, DB 설계, 시스템 구조 문서
│
└── README.md
```

## 폴더별 역할

### 1. spring-server

Spring Boot 기반의 메인 백엔드 서버입니다.

주요 역할은 다음과 같습니다.

* 회원가입 및 로그인
* 사용자 권한 관리
* 케이지 등록 및 조회
* 반려동물 정보 등록 및 조회
* 라즈베리파이 기기 등록 정보 관리
* MAC 주소와 IP 주소 저장
* 케이지와 기기 매칭
* 케이지별 영상 스트리밍 URL 제공
* 센서 로그 저장 및 조회

프론트엔드는 기본적으로 Spring Boot 서버의 API를 호출합니다.

예시:

```http
GET /api/cages/{cageId}/stream
```

응답 예시:

```json
{
  "cageId": 1,
  "streamUrl": "http://192.168.0.15:8001/video_feed"
}
```

### 2. fastapi-stream-server

Python/FastAPI 기반의 영상 스트리밍 및 AI 분석 서버입니다.

주요 역할은 다음과 같습니다.

* 라즈베리파이 카메라 영상 송출
* 실시간 영상 스트리밍 처리
* 센서 데이터 처리
* YOLO 기반 행동 분석
* AI 리포트 생성 기능 연동

영상 처리와 AI 분석은 Python 환경이 더 적합하므로 Spring Boot 서버와 분리하여 관리합니다.

### 3. raspberry-pi

라즈베리파이에서 직접 실행하는 코드가 위치합니다.

주요 역할은 다음과 같습니다.

* 라즈베리파이의 IP 주소 확인
* 라즈베리파이의 MAC 주소 확인
* 서버로 기기 정보 전송
* 센서 데이터 수집 및 전송
* 카메라 실행 관련 코드 관리

현재 `register_device.py`는 라즈베리파이의 IP/MAC 주소를 서버로 전송하여 기기 등록에 활용하는 코드입니다.

### 4. docs

프로젝트 문서가 위치합니다.

작성 예정 문서는 다음과 같습니다.

* API 명세서
* DB 테이블 설계
* 시스템 아키텍처
* 라즈베리파이 연동 방법
* 실행 방법 및 배포 방법

## 전체 동작 흐름

```text
1. 라즈베리파이가 자신의 IP/MAC 주소를 서버에 등록한다.
2. Spring Boot 서버는 기기 정보를 DB에 저장하거나 갱신한다.
3. 관리자는 케이지와 라즈베리파이 기기를 매칭한다.
4. 사용자가 프론트에서 특정 케이지 화면을 클릭한다.
5. 프론트는 Spring Boot 서버에 해당 케이지의 영상 URL을 요청한다.
6. Spring Boot 서버는 연결된 라즈베리파이의 영상 스트리밍 주소를 반환한다.
7. 프론트는 반환받은 영상 URL을 화면에 표시한다.
```

## 개발 방향

본 프로젝트는 다음과 같은 방식으로 개발을 진행합니다.

* Spring Boot는 중앙 백엔드 서버 역할을 담당합니다.
* FastAPI는 영상 스트리밍 및 AI 분석 기능을 담당합니다.
* Raspberry Pi 코드는 실제 하드웨어에서 실행되는 코드로 분리합니다.
* 프론트엔드는 Spring Boot API를 기준으로 데이터를 요청합니다.
* 영상 스트리밍, 센서 데이터, AI 분석 결과는 필요에 따라 Spring Boot 서버와 연동합니다.

## 기술 스택

### Backend

* Java
* Spring Boot
* Spring Data JPA
* PostgreSQL

### Streaming / AI

* Python
* FastAPI
* OpenCV
* YOLO

### Hardware

* Raspberry Pi
* Camera Module
* Sensor Module

## 현재 진행 상태

* 라즈베리파이 IP/MAC 정보 전송 코드 추가
* Spring Boot 서버 프로젝트 구조 추가
* 백엔드 서버 구조 분리 예정
