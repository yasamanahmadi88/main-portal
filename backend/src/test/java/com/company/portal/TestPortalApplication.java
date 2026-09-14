package com.company.portal;

import org.springframework.boot.SpringApplication;

/**
 * Test application entry point. Uses AbstractIntegrationTest pattern for
 * Testcontainers management instead of @TestConfiguration beans.
 */
public class TestPortalApplication {

	public static void main(String[] args) {
		SpringApplication.from(PortalApplication::main).run(args);
	}

}
