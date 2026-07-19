package com.company.portal.observability.web;

import com.company.portal.observability.MonitoringQueryService;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Permission-gated monitoring summaries for the SPA {@code /monitoring} surface.
 * Raw Prometheus/Grafana/Loki/Tempo UIs remain on the observability overlay network.
 */
@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

    private final MonitoringQueryService queryService;

    public MonitoringController(MonitoringQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('monitoring:read')")
    public Map<String, Object> overview() {
        return queryService.overview();
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAuthority('monitoring:metrics:read')")
    public Map<String, Object> metrics() {
        return queryService.metricsSummary();
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('monitoring:logs:read')")
    public Map<String, Object> logs() {
        return queryService.logsSummary();
    }

    @GetMapping("/traces")
    @PreAuthorize("hasAuthority('monitoring:traces:read')")
    public Map<String, Object> traces() {
        return queryService.tracesSummary();
    }
}
