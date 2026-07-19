package com.company.portal.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for full integration tests. Provisions ephemeral PostgreSQL and
 * Redis containers via Testcontainers and wires Spring Boot properties to
 * point at them.
 *
 * <p>Containers start only when Docker can actually run them. Subclasses must
 * also use {@code @EnabledIf(DockerAvailability.class)} so Spring Boot does
 * not attempt context startup when Docker is unusable.</p>
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("portal")
                    .withUsername("portal")
                    .withPassword("portal")
                    .withReuse(false);

    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
                    .withExposedPorts(6379)
                    .withReuse(false);

    static {
        // Start only when a real container can be created. Class loading for
        // disabled (@EnabledIf false) tests still executes this block, so the
        // probe must be cheap after the first call (cached).
        if (DockerAvailability.isAvailable()) {
            POSTGRES.start();
            REDIS.start();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.docker.compose.enabled", () -> "false");
    }
}
