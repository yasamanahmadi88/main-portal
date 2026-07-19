package com.company.portal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the full application context against Testcontainers-backed PostgreSQL
 * and Redis. Skipped when the Docker daemon is not reachable so that
 * CI environments without Docker (linters, IDE full-project checks) don't
 * spuriously fail.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnabledIf(value = "com.company.portal.support.DockerAvailability#isAvailable",
        disabledReason = "Docker is not available in this environment.")
class PortalApplicationTests {

    @Test
    void contextLoads() {
    }
}
