package com.company.portal.shared.security;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * All permission codes seeded in V2__rbac.sql. Kept as an enum so callers can
 * reference them symbolically rather than via magic strings when authoring
 * {@code @PreAuthorize("hasAuthority(...)")} or programmatic checks.
 */
public enum PortalPermission {

    USER_READ("user:read"),
    USER_WRITE("user:write"),
    USER_DELETE("user:delete"),
    USER_IMPERSONATE("user:impersonate"),
    ROLE_READ("role:read"),
    ROLE_WRITE("role:write"),
    PERMISSION_READ("permission:read"),
    PERMISSION_WRITE("permission:write"),
    AUDIT_READ("audit:read"),
    AUDIT_EXPORT("audit:export"),
    SECURITY_READ("security:read"),
    SECURITY_WRITE("security:write"),
    SESSION_READ("session:read"),
    SESSION_REVOKE("session:revoke"),
    SETTINGS_READ("settings:read"),
    SETTINGS_WRITE("settings:write"),
    NOTIFICATION_SEND("notification:send"),
    MFA_MANAGE("mfa:manage"),
    SELF_READ("self:read"),
    SELF_WRITE("self:write"),
    MONITORING_READ("monitoring:read"),
    MONITORING_METRICS_READ("monitoring:metrics:read"),
    MONITORING_LOGS_READ("monitoring:logs:read"),
    MONITORING_TRACES_READ("monitoring:traces:read");

    private static final Map<String, PortalPermission> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(PortalPermission::code, p -> p));

    private final String code;

    PortalPermission(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public String authority() {
        return code;
    }

    public static Optional<PortalPermission> fromCode(String code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
