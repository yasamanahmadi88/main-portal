package com.company.portal.shared.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * Production Redis configuration with authentication and persistence hardening.
 *
 * PRODUCTION SECURITY REQUIREMENTS:
 *   1. Authentication: Redis must require strong password (requirepass)
 *      Environment: SPRING_DATA_REDIS_PASSWORD (use secret manager, never in code)
 *
 *   2. Network Binding: Redis MUST bind to private interfaces only
 *      Configuration: redis.conf bind 10.0.0.5  (or equivalent private IP)
 *                    Never: bind 0.0.0.0
 *
 *   3. TLS Support: Enable for client connections when Redis lives outside VPC
 *      Environment: SPRING_DATA_REDIS_SSL=true + certificate paths
 *
 *   4. Persistence Strategy:
 *      - RDB (snapshotting): Basic backup, fast startup
 *      - AOF (append-only): Continuous log, higher durability
 *      PRODUCTION: Enable both for compliance and recovery
 *        redis.conf:
 *          save 900 1
 *          save 300 10
 *          save 60 10000
 *          appendonly yes
 *          appendfsync everysec
 *
 *   5. Memory Limits & Eviction:
 *      redis.conf:
 *        maxmemory 512mb  (set to 80% of actual available)
 *        maxmemory-policy allkeys-lru  (or noeviction if sessions are critical)
 *
 *   6. Monitoring (Prometheus):
 *      Enable via spring.management.metrics.enable.redis=true
 *      Expose /actuator/prometheus on private network only
 *
 * BACKUP & RECOVERY:
 *   Automated backups of RDB/AOF dumps every 6 hours to S3-compatible storage
 *   Restore procedure:
 *     1. Stop Redis instance
 *     2. Copy dump.rdb (or appendonly.aof) from backup to Redis data directory
 *     3. Start Redis instance
 *   Test recovery monthly in staging environment
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "portal.redis",
    name = "authentication-required",
    havingValue = "true"
)
public class ProductionRedisConfig {

    private final PortalProperties portalProperties;

    public ProductionRedisConfig(PortalProperties portalProperties) {
        this.portalProperties = portalProperties;
    }

    /**
     * Production-hardened Redis connection factory with authentication.
     *
     * @return configured LettuceConnectionFactory with auth and TLS if enabled
     */
    @Bean
    public RedisConnectionFactory productionRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        // Authentication (required in production)
        PortalProperties.Redis redisProps = portalProperties.getRedis();
        if (redisProps.isAuthenticationRequired()) {
            String password = redisProps.getPassword();
            if (password != null && !password.isBlank()) {
                config.setPassword(password);
            }
        }

        // TLS support (for Redis outside VPC)
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
            LettuceClientConfiguration.builder();

        if (redisProps.isTlsEnabled()) {
            builder.useSsl();
        }

        return new LettuceConnectionFactory(config, builder.build());
    }
}
