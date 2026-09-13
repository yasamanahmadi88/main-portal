package com.company.portal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import com.company.portal.support.AbstractIntegrationTest;

/**
 * Boots the full application context against Testcontainers-backed PostgreSQL
 * and Redis via AbstractIntegrationTest. Skipped when the Docker daemon is not
 * reachable so that CI environments without Docker (linters, IDE full-project
 * checks) don't spuriously fail.
 */
@EnabledIf(value = "com.company.portal.support.DockerAvailability#isAvailable",
        disabledReason = "Docker is not available in this environment.")
class PortalApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
