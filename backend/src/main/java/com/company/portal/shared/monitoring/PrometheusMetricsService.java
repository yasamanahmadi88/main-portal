package com.company.portal.shared.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

/**
 * Production metrics service for security monitoring and alerting.
 *
 * PRODUCTION ALERTS TO CONFIGURE:
 *
 * 1. LOGIN FAILURES SPIKE
 *    Alert if: rate(portal_login_failures_total[5m]) > 5
 *    Reason: Potential brute-force attack or credential stuffing
 *    Action: Trigger CAPTCHA, rate-limit by IP, notify security team
 *
 * 2. ACCOUNT LOCKOUTS SPIKE
 *    Alert if: rate(portal_account_lockouts_total[5m]) > 2
 *    Reason: Distributed attack targeting multiple accounts
 *    Action: Increase login rate limits, notify security team
 *
 * 3. AUDIT PERSISTENCE FAILURES
 *    Alert if: increase(portal_audit_write_failures_total[5m]) > 0
 *    Reason: Audit events could not be written to database
 *    Action: CRITICAL - investigate database connectivity, restore backups if needed
 *
 * 4. REDIS UNAVAILABLE
 *    Alert if: up{job="redis"} == 0 OR
 *              redis_connected_clients == 0 OR
 *              rate(redis_commands_failed_calls_total[5m]) > 1
 *    Reason: Session storage or cache is offline
 *    Action: Investigate Redis, trigger failover if applicable
 *
 * 5. POSTGRES CONNECTION EXHAUSTION
 *    Alert if: pg_stat_activity_count / pg_settings_max_connections > 0.9
 *    Reason: Database connection pool nearly full
 *    Action: Scale database connections, investigate slow queries
 *
 * 6. AUDIT CHAIN VERIFICATION MISMATCH
 *    Alert if: increase(portal_audit_chain_verification_failures_total[5m]) > 0
 *    Reason: Hash chain verification failed (possible tampering detection)
 *    Action: CRITICAL - investigate audit table integrity, enable forensics
 *
 * METRICS EXPOSURE:
 *   Endpoint: /actuator/prometheus (on management port, private network only)
 *   Format: Prometheus text format (OpenMetrics compatible)
 *   Scrape interval: 30s recommended
 *   Cardinality: ~200 metrics (keep bounded)
 *
 * @see https://micrometer.io/docs/registry/prometheus
 */
@Service
@ConditionalOnClass(MeterRegistry.class)
public class PrometheusMetricsService {
    private static final Logger logger = LoggerFactory.getLogger(PrometheusMetricsService.class);

    private final MeterRegistry meterRegistry;

    // Counters for security events (never decreases, always accumulates)
    private final Counter loginFailuresCounter;
    private final Counter accountLockoutsCounter;
    private final Counter auditWriteFailuresCounter;
    private final Counter auditChainVerificationFailuresCounter;

    // Timers for performance monitoring
    private final Timer auditWriteTimer;
    private final Timer databaseConnectionTimer;

    public PrometheusMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Security event counters (production alerting triggers)
        Tags baseTags = Tags.of("service", "portal");

        this.loginFailuresCounter = Counter.builder("portal_login_failures_total")
            .description("Total failed login attempts")
            .tags(baseTags)
            .register(meterRegistry);

        this.accountLockoutsCounter = Counter.builder("portal_account_lockouts_total")
            .description("Total account lockouts due to failed login attempts")
            .tags(baseTags)
            .register(meterRegistry);

        this.auditWriteFailuresCounter = Counter.builder("portal_audit_write_failures_total")
            .description("Failed attempts to write audit events to database (CRITICAL alert trigger)")
            .tags(baseTags)
            .register(meterRegistry);

        this.auditChainVerificationFailuresCounter = Counter.builder("portal_audit_chain_verification_failures_total")
            .description("Hash chain verification failures (tampering detection)")
            .tags(baseTags)
            .register(meterRegistry);

        // Performance monitoring
        this.auditWriteTimer = Timer.builder("portal_audit_write_duration_seconds")
            .description("Time to persist audit events to database")
            .tags(baseTags)
            .register(meterRegistry);

        this.databaseConnectionTimer = Timer.builder("portal_database_connection_duration_seconds")
            .description("Time to obtain database connection from pool")
            .tags(baseTags)
            .register(meterRegistry);
    }

    /**
     * Record a failed login attempt.
     * Alert if rate exceeds threshold (5/min) to detect brute-force attacks.
     */
    public void recordLoginFailure(String reason) {
        loginFailuresCounter.increment();
        logger.warn("Login failure recorded: {}", reason,
            new SecurityEventMarker("LOGIN_FAILURE", reason));
    }

    /**
     * Record an account lockout.
     * Alert if rate exceeds threshold (2/min) to detect distributed attacks.
     */
    public void recordAccountLockout(String userId, String reason) {
        accountLockoutsCounter.increment();
        logger.warn("Account lockout recorded for user {}: {}",
            userId, reason,
            new SecurityEventMarker("ACCOUNT_LOCKOUT", userId));
    }

    /**
     * Record a failure to persist audit events.
     * CRITICAL: This indicates the audit trail is compromised.
     * Alert immediately on any occurrence.
     */
    public void recordAuditWriteFailure(String table, Exception cause) {
        auditWriteFailuresCounter.increment();
        logger.error("CRITICAL: Failed to write audit event to table {}: {}",
            table, cause.getMessage(), cause,
            new SecurityEventMarker("AUDIT_WRITE_FAILURE", table));
    }

    /**
     * Record a hash chain verification failure.
     * This indicates the audit trail may have been tampered with.
     * Alert immediately for forensics investigation.
     */
    public void recordAuditChainVerificationFailure(long eventId, String reason) {
        auditChainVerificationFailuresCounter.increment();
        logger.error("CRITICAL: Audit chain verification failed for event {}: {}",
            eventId, reason,
            new SecurityEventMarker("AUDIT_CHAIN_MISMATCH", String.valueOf(eventId)));
    }

    /**
     * Record audit event write performance.
     * High latency may indicate database bottlenecks or connection pool exhaustion.
     */
    public void recordAuditWriteDuration(long durationNanos) {
        auditWriteTimer.record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    /**
     * Record database connection acquisition time.
     * High values indicate connection pool contention.
     */
    public void recordDatabaseConnectionDuration(long durationNanos) {
        databaseConnectionTimer.record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    /**
     * Gauge: Current Redis connection pool status.
     * If this reaches 0 (disconnected), it indicates session storage is unavailable.
     * Must be called periodically from a health check.
     */
    public void recordRedisConnectionPoolStatus(int activeConnections, int maxConnections) {
        meterRegistry.gauge("portal_redis_pool_active_connections",
            Tags.of("service", "portal"),
            activeConnections);
        meterRegistry.gauge("portal_redis_pool_max_connections",
            Tags.of("service", "portal"),
            maxConnections);
    }

    /**
     * Internal marker class for structured logging of security events.
     * Integrates with observability stack for filtering and alerting.
     */
    private static class SecurityEventMarker {
        private final String eventType;
        private final String details;

        SecurityEventMarker(String eventType, String details) {
            this.eventType = eventType;
            this.details = details;
        }

        @Override
        public String toString() {
            return String.format("[SECURITY_EVENT %s] %s", eventType, details);
        }
    }
}
