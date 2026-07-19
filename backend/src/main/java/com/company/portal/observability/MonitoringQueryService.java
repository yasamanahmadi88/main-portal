package com.company.portal.observability;

import com.company.portal.shared.config.PortalProperties;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Builds sanitized monitoring snapshots for the SPA. Never includes passwords,
 * tokens, session ids, MFA secrets, or raw PII.
 */
@Service
public class MonitoringQueryService {

    private final HealthEndpoint healthEndpoint;
    private final RedisConnectionFactory redisConnectionFactory;
    private final PortalProperties properties;
    private final JdbcTemplate jdbc;

    public MonitoringQueryService(HealthEndpoint healthEndpoint,
                                  RedisConnectionFactory redisConnectionFactory,
                                  PortalProperties properties,
                                  JdbcTemplate jdbc) {
        this.healthEndpoint = healthEndpoint;
        this.redisConnectionFactory = redisConnectionFactory;
        this.properties = properties;
        this.jdbc = jdbc;
    }

    public Map<String, Object> overview() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("generatedAt", Instant.now().toString());
        body.put("application", applicationHealth());
        body.put("jvm", jvmSnapshot());
        body.put("database", databaseSnapshot());
        body.put("redis", redisSnapshot());
        body.put("authentication", authSnapshot());
        body.put("securityEvents", securitySnapshot());
        body.put("audit", auditSnapshot());
        body.put("links", externalLinks());
        return body;
    }

    public Map<String, Object> metricsSummary() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("generatedAt", Instant.now().toString());
        body.put("jvm", jvmSnapshot());
        body.put("application", applicationHealth());
        body.put("performance", Map.of(
                "processors", Runtime.getRuntime().availableProcessors(),
                "uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000L
        ));
        return body;
    }

    public Map<String, Object> logsSummary() {
        Long securityCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM security_events WHERE occurred_at > NOW() - INTERVAL '24 hours'",
                Long.class);
        Long auditCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE occurred_at > NOW() - INTERVAL '24 hours'",
                Long.class);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("generatedAt", Instant.now().toString());
        body.put("window", "24h");
        body.put("securityEventCount", securityCount == null ? 0 : securityCount);
        body.put("auditEventCount", auditCount == null ? 0 : auditCount);
        body.put("note", "Raw log streams are available in Loki when the observability overlay is running.");
        return body;
    }

    public Map<String, Object> tracesSummary() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("generatedAt", Instant.now().toString());
        body.put("otelEnabled", Boolean.parseBoolean(
                System.getenv().getOrDefault("TELEMETRY_ENABLED", "false")));
        body.put("note", "Distributed traces are available in Tempo when the observability overlay is running.");
        body.put("correlationHint", "Use X-Correlation-ID / trace_id fields in structured logs.");
        String prometheus = properties.getMonitoring().getPrometheusPublicUrl();
        if (prometheus != null && !prometheus.isBlank()) {
            body.put("prometheusUrl", prometheus);
        }
        return body;
    }

    private Map<String, Object> applicationHealth() {
        Status status = healthEndpoint.health().getStatus();
        return Map.of(
                "status", status == null ? Status.UNKNOWN.getCode() : status.getCode(),
                "components", List.of("backend", "postgres", "redis")
        );
    }

    private Map<String, Object> jvmSnapshot() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        long heapUsed = memory.getHeapMemoryUsage().getUsed();
        long heapMax = memory.getHeapMemoryUsage().getMax();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("uptimeSeconds", runtime.getUptime() / 1000L);
        body.put("heapUsedBytes", heapUsed);
        body.put("heapMaxBytes", heapMax);
        body.put("heapUsageRatio", heapMax > 0 ? (double) heapUsed / heapMax : 0d);
        body.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
        return body;
    }

    private Map<String, Object> databaseSnapshot() {
        try {
            Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
            Long activeSessions = jdbc.queryForObject(
                    """
                    SELECT COUNT(*) FROM user_session_metadata
                    WHERE revoked_at IS NULL
                      AND (expires_at IS NULL OR expires_at > NOW())
                    """,
                    Long.class);
            return Map.of(
                    "status", one != null && one == 1 ? "UP" : "DOWN",
                    "activeSessions", activeSessions == null ? 0 : activeSessions
            );
        } catch (RuntimeException ex) {
            return Map.of("status", "DOWN");
        }
    }

    private Map<String, Object> redisSnapshot() {
        try (var connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            return Map.of("status", "PONG".equalsIgnoreCase(pong) ? "UP" : "UNKNOWN");
        } catch (RuntimeException ex) {
            return Map.of("status", "DOWN");
        }
    }

    private Map<String, Object> authSnapshot() {
        Long failures24h = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM login_attempts
                WHERE attempted_at > NOW() - INTERVAL '24 hours'
                  AND outcome IN ('FAILURE', 'LOCKED', 'MFA_FAILED')
                """,
                Long.class);
        Long successes24h = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM login_attempts
                WHERE attempted_at > NOW() - INTERVAL '24 hours'
                  AND outcome = 'SUCCESS'
                """,
                Long.class);
        return Map.of(
                "window", "24h",
                "failures", failures24h == null ? 0 : failures24h,
                "successes", successes24h == null ? 0 : successes24h
        );
    }

    private Map<String, Object> securitySnapshot() {
        Long openHigh = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM security_events
                WHERE acknowledged_at IS NULL
                  AND severity IN ('ERROR', 'CRITICAL')
                """,
                Long.class);
        return Map.of("unacknowledgedHighOrCritical", openHigh == null ? 0 : openHigh);
    }

    private Map<String, Object> auditSnapshot() {
        Long last24h = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE occurred_at > NOW() - INTERVAL '24 hours'",
                Long.class);
        return Map.of("eventsLast24h", last24h == null ? 0 : last24h);
    }

    private Map<String, Object> externalLinks() {
        Map<String, Object> links = new LinkedHashMap<>();
        String grafana = properties.getMonitoring().getGrafanaPublicUrl();
        String prometheus = properties.getMonitoring().getPrometheusPublicUrl();
        if (grafana != null && !grafana.isBlank()) {
            links.put("grafana", grafana);
        }
        if (prometheus != null && !prometheus.isBlank()) {
            links.put("prometheus", prometheus);
        }
        return links;
    }
}
