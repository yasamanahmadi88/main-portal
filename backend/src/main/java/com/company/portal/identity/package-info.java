/**
 * Identity module: user account lifecycle, authentication, session
 * management, password recovery, and MFA (TOTP + recovery codes).
 * Depends on the shared platform, access control, audit, security-event,
 * notification, and observability (auth metrics) modules.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Identity"
)
package com.company.portal.identity;
