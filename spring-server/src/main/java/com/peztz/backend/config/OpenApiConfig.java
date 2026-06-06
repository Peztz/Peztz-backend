package com.peztz.backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI peztzOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Peztz Backend API")
						.description("Peztz 프로젝트의 Spring Boot REST API 문서입니다. 라즈베리파이 등록, 장치 목록 조회, 스트리밍 URL 조회 API를 포함합니다.")
						.version("v1.0.0"))
				.servers(List.of(
						new Server()
								.url("http://localhost:8080")
								.description("local"),
						new Server()
								.url("http://34.50.7.78:8080")
								.description("production")));
	}
}
