package com.peztz.backend.smartthings.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "smartthings")
public class SmartThingsProperties {

	private String baseUrl = "https://api.smartthings.com/v1";
	private String accessToken = "";
	private BigDecimal lowLightThresholdLux = BigDecimal.valueOf(50);

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public BigDecimal getLowLightThresholdLux() {
		return lowLightThresholdLux;
	}

	public void setLowLightThresholdLux(BigDecimal lowLightThresholdLux) {
		this.lowLightThresholdLux = lowLightThresholdLux;
	}
}
