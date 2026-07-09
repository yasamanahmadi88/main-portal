package com.company.portal.dashboard.web;

import com.company.portal.dashboard.application.DashboardQueryService;
import com.company.portal.dashboard.application.DashboardQueryService.DashboardSummary;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardQueryService queryService;

    public DashboardController(DashboardQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('self:read')")
    public DashboardSummary summary() {
        return queryService.summary();
    }
}
