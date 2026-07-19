package com.company.portal.shared.error;

/**
 * Stable machine-readable error codes. These become the ``code`` extension on
 * {@link org.springframework.http.ProblemDetail} responses and are safe for
 * clients to switch on. Adding a new code is always backward compatible;
 * changing or removing one is a breaking change.
 */
public final class ErrorCodes {

    private ErrorCodes() { }

    public static final String VALIDATION_FAILED   = "validation_failed";
    public static final String NOT_FOUND           = "not_found";
    public static final String CONFLICT            = "conflict";
    public static final String UNAUTHENTICATED     = "unauthenticated";
    public static final String FORBIDDEN           = "forbidden";
    public static final String RATE_LIMITED        = "rate_limited";
    public static final String INTERNAL_ERROR      = "internal_error";
    public static final String BAD_REQUEST         = "bad_request";
    public static final String METHOD_NOT_ALLOWED  = "method_not_allowed";
    public static final String UNSUPPORTED_MEDIA   = "unsupported_media_type";
    public static final String PAYLOAD_TOO_LARGE   = "payload_too_large";
    public static final String OPTIMISTIC_LOCK     = "optimistic_lock_failure";
    public static final String MFA_REQUIRED        = "mfa_required";
    public static final String MFA_INVALID         = "mfa_invalid";
    public static final String ACCOUNT_LOCKED      = "account_locked";
    public static final String PASSWORD_POLICY     = "password_policy_violation";
    public static final String CSRF_INVALID        = "csrf_invalid";
    public static final String CAPTCHA_INVALID     = "captcha_invalid";
    public static final String CAPTCHA_EXPIRED     = "captcha_expired";
    public static final String CAPTCHA_REQUIRED    = "captcha_required";
}
