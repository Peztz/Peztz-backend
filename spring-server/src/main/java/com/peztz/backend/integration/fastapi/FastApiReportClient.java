package com.peztz.backend.integration.fastapi;

public interface FastApiReportClient {

	FastApiReportGenerationResponse generate(FastApiReportGenerationRequest request);
}
