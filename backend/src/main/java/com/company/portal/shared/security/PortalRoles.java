package com.company.portal.shared.security;

/**
 * System-level role codes matching seeded rows in V2__rbac.sql.
 * Codes appear on user_roles and in Spring Security authorities prefixed
 * with {@code ROLE_}.
 */
public final class PortalRoles {

    private PortalRoles() { }

    public static final String SUPER_ADMIN    = "SUPER_ADMIN";
    public static final String ADMIN          = "ADMIN";
    public static final String SECURITY_ADMIN = "SECURITY_ADMIN";
    public static final String AUDITOR        = "AUDITOR";
    public static final String USER           = "USER";

    public static final String ROLE_PREFIX = "ROLE_";

    public static String authority(String role) {
        return ROLE_PREFIX + role;
    }
}
