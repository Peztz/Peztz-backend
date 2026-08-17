package com.peztz.backend.smartthings.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peztz.backend.smartthings.config.SmartThingsProperties;
import com.peztz.backend.smartthings.exception.SmartThingsApiException;

class SmartThingsClientTest {

	private static final String BASE_URL = "https://smartthings.example/v1";
	private static final String ACCESS_TOKEN = "test-smartthings-token";

	private MockRestServiceServer server;
	private SmartThingsClient client;

	@BeforeEach
	void setUp() {
		SmartThingsProperties properties = new SmartThingsProperties();
		properties.setBaseUrl(BASE_URL);
		RestClient.Builder restClientBuilder = RestClient.builder();
		server = MockRestServiceServer.bindTo(restClientBuilder).build();
		client = new SmartThingsClient(restClientBuilder, new ObjectMapper(), properties);
	}

	@Test
	void parsesDeviceListResponse() {
		server.expect(once(), requestTo(BASE_URL + "/devices"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
				.andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
				.andRespond(withSuccess("""
						{
						  "items": [
						    {
						      "deviceId": "device-1",
						      "name": "temperature-sensor",
						      "label": "Room Sensor",
						      "manufacturerName": "SmartThings",
						      "components": [{"id": "main"}],
						      "customField": "preserved"
						    }
						  ],
						  "paging": {"next": "cursor"},
						  "_links": {"next": {"href": "/devices?page=2"}}
						}
						""", MediaType.APPLICATION_JSON));

		JsonNode response = client.getDevices(ACCESS_TOKEN);

		assertThat(response.path("items").get(0).path("deviceId").asText()).isEqualTo("device-1");
		assertThat(response.path("items").get(0).path("customField").asText()).isEqualTo("preserved");
		assertThat(response.path("paging").path("next").asText()).isEqualTo("cursor");
		server.verify();
	}

	@Test
	void parsesDeviceStatusResponse() {
		server.expect(once(), requestTo(BASE_URL + "/devices/device-1/status"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("""
						{
						  "components": {
						    "main": {
						      "temperatureMeasurement": {
						        "temperature": {"value": 23.2, "unit": "C"}
						      },
						      "relativeHumidityMeasurement": {
						        "humidity": {"value": 55}
						      },
						      "customCapability": {
						        "customAttribute": {"value": "kept"}
						      }
						    }
						  }
						}
						""", MediaType.APPLICATION_JSON));

		JsonNode response = client.getDeviceStatus(ACCESS_TOKEN, "device-1");

		assertThat(response.path("components").path("main").path("temperatureMeasurement")
				.path("temperature").path("value").asDouble()).isEqualTo(23.2);
		assertThat(response.path("components").path("main").path("customCapability")
				.path("customAttribute").path("value").asText()).isEqualTo("kept");
		server.verify();
	}

	@Test
	void parsesDeviceHealthResponse() {
		server.expect(once(), requestTo(BASE_URL + "/devices/device-1/health"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
				.andRespond(withSuccess("""
						{
						  "deviceId": "device-1",
						  "state": "ONLINE",
						  "lastUpdatedDate": "2026-08-17T00:00:00Z"
						}
						""", MediaType.APPLICATION_JSON));

		JsonNode response = client.getDeviceHealth(ACCESS_TOKEN, "device-1");

		assertThat(response.path("state").asText()).isEqualTo("ONLINE");
		server.verify();
	}

	@Test
	void mapsSmartThings401() {
		expectStatus(HttpStatus.UNAUTHORIZED);

		assertThatThrownBy(() -> client.getDevices(ACCESS_TOKEN))
				.isInstanceOfSatisfying(SmartThingsApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
					assertThat(exception.getCode()).isEqualTo("SMARTTHINGS_UNAUTHORIZED");
				});
	}

	@Test
	void mapsSmartThings403() {
		expectStatus(HttpStatus.FORBIDDEN);

		assertThatThrownBy(() -> client.getDevices(ACCESS_TOKEN))
				.isInstanceOfSatisfying(SmartThingsApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
					assertThat(exception.getCode()).isEqualTo("SMARTTHINGS_FORBIDDEN");
				});
	}

	@Test
	void mapsSmartThings429() {
		expectStatus(HttpStatus.TOO_MANY_REQUESTS);

		assertThatThrownBy(() -> client.getDevices(ACCESS_TOKEN))
				.isInstanceOfSatisfying(SmartThingsApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
					assertThat(exception.getCode()).isEqualTo("SMARTTHINGS_RATE_LIMITED");
				});
	}

	@Test
	void mapsSmartThings404() {
		expectStatus(HttpStatus.NOT_FOUND);

		assertThatThrownBy(() -> client.getDevices(ACCESS_TOKEN))
				.isInstanceOfSatisfying(SmartThingsApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
					assertThat(exception.getCode()).isEqualTo("SMARTTHINGS_NOT_FOUND");
				});
	}

	@Test
	void mapsSmartThings5xx() {
		expectStatus(HttpStatus.INTERNAL_SERVER_ERROR);

		assertThatThrownBy(() -> client.getDevices(ACCESS_TOKEN))
				.isInstanceOfSatisfying(SmartThingsApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
					assertThat(exception.getCode()).isEqualTo("SMARTTHINGS_SERVER_ERROR");
				});
	}

	@Test
	void mapsNetworkError() {
		server.expect(once(), requestTo(BASE_URL + "/devices"))
				.andRespond(withException(new IOException("network unavailable")));

		assertThatThrownBy(() -> client.getDevices(ACCESS_TOKEN))
				.isInstanceOfSatisfying(SmartThingsApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
					assertThat(exception.getCode()).isEqualTo("SMARTTHINGS_NETWORK_ERROR");
				});
	}

	@Test
	void mapsInvalidJsonResponse() {
		server.expect(once(), requestTo(BASE_URL + "/devices"))
				.andRespond(withSuccess("{invalid-json", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.getDevices(ACCESS_TOKEN))
				.isInstanceOfSatisfying(SmartThingsApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
					assertThat(exception.getCode()).isEqualTo("SMARTTHINGS_INVALID_RESPONSE");
				});
	}

	private void expectStatus(HttpStatus status) {
		server.expect(once(), requestTo(BASE_URL + "/devices"))
				.andRespond(withStatus(status)
						.contentType(MediaType.APPLICATION_JSON)
						.body("{}"));
	}
}
