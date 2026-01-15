package com.yunlbd.flexboot4;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
class Flexboot4ApplicationTests {

	@Test
	void contextLoads() {
	}

	@Configuration
	static class TestConfig {
		@Bean
		public JavaMailSender javaMailSender() {
			return null; // Mock bean for tests
		}
	}
}
