package com.peztz.backend.device.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "라즈베리파이 스트리밍 URL 응답")
public class RaspberryPiStreamResponse {

	@Schema(description = "라즈베리파이 장치 UUID", example = "1b03c87c-0f82-4b26-8f23-f4b6cfd8f3a1")
	private UUID deviceId;

	@Schema(description = "라즈베리파이 MAC 주소", example = "88:A2:9E:3D:02:BD")
	private String macAddress;

	@Schema(description = "마지막으로 등록된 라즈베리파이 IP 주소", example = "192.168.150.142")
	private String lastIp;

	@Schema(description = "MJPEG 스트리밍 URL", example = "http://192.168.150.142:8001/video_feed")
	private String streamUrl;
}
