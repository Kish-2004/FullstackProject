package com.example.courseservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"course-service.greeting=Hello from test config!",
		"some.other.property=value"
})
public class CourseServiceConfigTest {

	// Inject the property value
	@Value("${course-service.greeting}")
	private String greetingMessage;

	@Value("${some.other.property}")
	private String otherProperty;

	@Test
	void contextLoadsAndLoadsTestProperties() {
		assertThat(greetingMessage).isEqualTo("Hello from test config!");
		assertThat(otherProperty).isEqualTo("value");
	}
}