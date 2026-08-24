package com.peztz.backend.integration.fastapi;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "peztz.fastapi.client-mode", havingValue = "mock", matchIfMissing = true)
public class MockFastApiReportClient implements FastApiReportClient {

	@Override
	public FastApiReportGenerationResponse generate(FastApiReportGenerationRequest request) {
		return new FastApiReportGenerationResponse(
				"오늘은 총 %d개의 관찰 기록이 수집되었습니다.".formatted(request.statistics().totalLogCount()),
				List.of(),
				new FastApiReportGenerationResponse.EnvironmentCard(
						"생활 환경",
						"개발 모드에서는 수집된 환경 통계만 표시합니다."),
				List.of("평소와 다른 행동이 보이면 직접 상태를 확인해 주세요."),
				"NORMAL",
				List.of("현재 리포트는 FastAPI mock 모드에서 생성되었습니다."),
				"이 리포트는 진단이 아닌 관찰 데이터 요약입니다.");
	}
}
