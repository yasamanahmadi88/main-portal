package com.company.portal.shared.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Base for all business-level exceptions raised by portal modules.
 *
 * <p>Concrete subclasses map to specific HTTP semantics. The {@link #getCode()}
 * value is a stable machine identifier (see {@link ErrorCodes}) and never
 * contains sensitive detail.</p>
 */
public class PortalException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    protected PortalException(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of(), null);
    }

    protected PortalException(HttpStatus status, String code, String message,
                              Map<String, Object> details) {
        this(status, code, message, details, null);
    }

    protected PortalException(HttpStatus status, String code, String message,
                              Map<String, Object> details, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.details = details == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public static final class NotFound extends PortalException {
        public NotFound(String message) {
            super(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, message);
        }
        public NotFound(String message, Map<String, Object> details) {
            super(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, message, details);
        }
    }

    public static final class Conflict extends PortalException {
        public Conflict(String message) {
            super(HttpStatus.CONFLICT, ErrorCodes.CONFLICT, message);
        }
        public Conflict(String message, Map<String, Object> details) {
            super(HttpStatus.CONFLICT, ErrorCodes.CONFLICT, message, details);
        }
    }

    public static final class Forbidden extends PortalException {
        public Forbidden(String message) {
            super(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, message);
        }
        public Forbidden(String code, String message) {
            super(HttpStatus.FORBIDDEN, code, message);
        }
    }

    public static final class Unauthorized extends PortalException {
        public Unauthorized(String message) {
            super(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHENTICATED, message);
        }
        public Unauthorized(String code, String message) {
            super(HttpStatus.UNAUTHORIZED, code, message);
        }
    }

    public static final class Validation extends PortalException {
        public Validation(String message) {
            super(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodes.VALIDATION_FAILED, message);
        }
        public Validation(String message, Map<String, Object> details) {
            super(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodes.VALIDATION_FAILED, message, details);
        }
    }

    public static final class RateLimited extends PortalException {
        private final long retryAfterSeconds;

        public RateLimited(String message, long retryAfterSeconds) {
            super(HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.RATE_LIMITED, message,
                    Map.of("retry_after_seconds", retryAfterSeconds));
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}
