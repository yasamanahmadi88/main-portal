package com.company.portal.administration.web;

import com.company.portal.shared.security.CurrentUserAccessor;
import com.company.portal.settings.application.SettingsService;
import com.company.portal.settings.application.SettingsService.SettingsSnapshot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final CurrentUserAccessor currentUser;

    public SettingsController(SettingsService settingsService, CurrentUserAccessor currentUser) {
        this.settingsService = settingsService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('settings:read')")
    public SettingsSnapshot get() {
        return settingsService.get();
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('settings:write')")
    public SettingsSnapshot patch(@RequestBody @Valid SettingsPatchRequest request) {
        Map<String, Object> updates = new LinkedHashMap<>();
        if (request.defaultLanguage() != null) updates.put("defaultLanguage", request.defaultLanguage());
        if (request.defaultTheme() != null) updates.put("defaultTheme", request.defaultTheme());
        if (request.sessionTimeoutMinutes() != null) updates.put("sessionTimeoutMinutes", request.sessionTimeoutMinutes());
        if (request.mfaRequiredForAdmins() != null) updates.put("mfaRequiredForAdmins", request.mfaRequiredForAdmins());
        if (request.auditRetentionDays() != null) updates.put("auditRetentionDays", request.auditRetentionDays());
        if (request.loginRateLimitPerMinute() != null) updates.put("loginRateLimitPerMinute", request.loginRateLimitPerMinute());
        return settingsService.patch(currentUser.currentUserIdOrThrow(), updates);
    }

    public record SettingsPatchRequest(
            String defaultLanguage,
            String defaultTheme,
            @Min(5) @Max(1440) Integer sessionTimeoutMinutes,
            Boolean mfaRequiredForAdmins,
            @Min(30) Integer auditRetentionDays,
            @Min(1) Integer loginRateLimitPerMinute) { }
}
