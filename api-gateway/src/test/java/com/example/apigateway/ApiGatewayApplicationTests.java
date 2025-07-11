package com.example.apigateway;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Removed: @Testcontainers // No longer needed as we're not using Testcontainers
public class ApiGatewayApplicationTests {

	@LocalServerPort
	private int port;

	private WebTestClient webClient;

	// Removed: Testcontainers for Eureka Server
	// Removed: @Container
	// Removed: public static GenericContainer<?> eurekaServer = new GenericContainer<>(DockerImageName.parse("springcloud/eureka"))
	// Removed:       .withExposedPorts(8761);

	// WireMock for mocking Course Service
	@RegisterExtension
	static WireMockExtension courseServiceMock = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	// WireMock for mocking Student Service
	@RegisterExtension
	static WireMockExtension studentServiceMock = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		// IMPORTANT: Disable Eureka client entirely for tests, as we are not using a real Eureka server.
		// The gateway will rely solely on the simple discovery properties below.
		registry.add("eureka.client.enabled", () -> "false");

		// Dynamically configure the gateway to resolve service IDs directly to WireMock URLs.
		// This uses Spring Cloud's SimpleDiscoveryClient, which is automatically enabled
		// when eureka.client.enabled is false and spring.cloud.discovery.client.simple.applications is configured.
		registry.add("spring.cloud.discovery.client.simple.applications.COURSE-SERVICE[0].uri", () ->
				courseServiceMock.baseUrl());
		registry.add("spring.cloud.discovery.client.simple.applications.COURSE-SERVICE[0].instanceId", () ->
				"course-service-mock-1");

		registry.add("spring.cloud.discovery.client.simple.applications.STUDENT-SERVICE[0].uri", () ->
				studentServiceMock.baseUrl());
		registry.add("spring.cloud.discovery.client.simple.applications.STUDENT-SERVICE[0].instanceId", () ->
				"student-service-mock-1");

		// Optional: You might uncomment this for debugging gateway routes
		// registry.add("logging.level.org.springframework.cloud.gateway", () -> "DEBUG");
	}

	@BeforeEach
	void setUp() {
		webClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
		courseServiceMock.resetAll();
		studentServiceMock.resetAll();
	}

	@Test
	void gatewayRoutesToCourseService() {
		courseServiceMock.stubFor(get(urlEqualTo("/api/courses/1"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"id\":1,\"title\":\"Test Course\",\"description\":\"Desc\"}")));

		webClient.get().uri("/api/courses/1")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueMatches("X-Gateway-Trace", "course-route") // Check our custom filter
				.expectBody()
				.jsonPath("$.title").isEqualTo("Test Course");

		courseServiceMock.verify(getRequestedFor(urlEqualTo("/api/courses/1")));
	}

	@Test
	void gatewayRoutesToStudentService() {
		studentServiceMock.stubFor(get(urlEqualTo("/api/students/10"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"id\":10,\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"jane@test.com\"}")));

		webClient.get().uri("/api/students/10")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueMatches("X-Gateway-Trace", "student-route") // Check our custom filter
				.expectBody()
				.jsonPath("$.firstName").isEqualTo("Jane");

		studentServiceMock.verify(getRequestedFor(urlEqualTo("/api/students/10")));
	}

	@Test
	void gatewayHealthCheckRouteForCourseService() {
		courseServiceMock.stubFor(get(urlEqualTo("/actuator/health"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"status\":\"UP\"}")));

		webClient.get().uri("/api/course-health")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("UP");

		courseServiceMock.verify(getRequestedFor(urlEqualTo("/actuator/health")));
	}

	@Test
	void corsFilterIsApplied() {
		// Test a preflight OPTIONS request
		webClient.options().uri("/api/courses/1")
				.header("Origin", "http://localhost:3000")
				.header("Access-Control-Request-Method", "GET")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().value("Access-Control-Allow-Origin", is("http://localhost:3000"))
				.expectHeader().value("Access-Control-Allow-Methods", is("GET,POST,PUT,DELETE,OPTIONS"))
				.expectHeader().value("Access-Control-Allow-Credentials", is("true"))
				.expectHeader().value("Access-Control-Max-Age", is("3600"));

		// Test a simple GET request with Origin header
		courseServiceMock.stubFor(get(urlEqualTo("/api/courses/1"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{}"))); // Dummy body

		webClient.get().uri("/api/courses/1")
				.header("Origin", "http://localhost:3000")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().value("Access-Control-Allow-Origin", is("http://localhost:3000"));
	}
}