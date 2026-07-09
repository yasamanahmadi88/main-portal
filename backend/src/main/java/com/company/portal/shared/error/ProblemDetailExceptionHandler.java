package com.company.portal.shared.error;

import com.company.portal.shared.logging.LogSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Central RFC 9457 {@link ProblemDetail} handler.
 *
 * <p>Never leaks stack traces, SQL, or third-party exception messages to
 * clients; leaves those details in structured server logs indexed by
 * ``correlation_id`` / ``trace_id``.</p>
 */
@RestControllerAdvice
public class ProblemDetailExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailExceptionHandler.class);
    private static final String TYPE_BASE = "https://portal.company.com/problems/";

    @ExceptionHandler(PortalException.class)
    public ResponseEntity<ProblemDetail> handlePortal(PortalException ex, HttpServletRequest request) {
        ProblemDetail body = build(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
        ex.getDetails().forEach(body::setProperty);
        HttpHeaders headers = new HttpHeaders();
        if (ex instanceof PortalException.RateLimited rl) {
            headers.add(HttpHeaders.RETRY_AFTER, Long.toString(rl.getRetryAfterSeconds()));
        }
        logExpected(ex, request);
        return ResponseEntity.status(ex.getStatus()).headers(headers).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, Object>> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> errors.add(Map.of(
                "field", fe.getField(),
                "code", fe.getCode() == null ? "invalid" : fe.getCode(),
                "message", LogSanitizer.safe(fe.getDefaultMessage()))));
        ex.getBindingResult().getGlobalErrors().forEach(oe -> errors.add(Map.of(
                "field", oe.getObjectName(),
                "code", oe.getCode() == null ? "invalid" : oe.getCode(),
                "message", LogSanitizer.safe(oe.getDefaultMessage()))));
        ProblemDetail body = build(HttpStatus.UNPROCESSABLE_ENTITY,
                ErrorCodes.VALIDATION_FAILED, "Request validation failed", request);
        body.setProperty("errors", errors);
        logExpected(ex, request);
        return ResponseEntity.unprocessableEntity().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<Map<String, Object>> errors = new ArrayList<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            errors.add(Map.of(
                    "field", v.getPropertyPath().toString(),
                    "code", "constraint",
                    "message", LogSanitizer.safe(v.getMessage())));
        }
        ProblemDetail body = build(HttpStatus.UNPROCESSABLE_ENTITY,
                ErrorCodes.VALIDATION_FAILED, "Request constraints violated", request);
        body.setProperty("errors", errors);
        logExpected(ex, request);
        return ResponseEntity.unprocessableEntity().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex, HttpServletRequest request) {
        ProblemDetail body = build(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST,
                "Malformed or missing request data", request);
        logExpected(ex, request);
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ProblemDetail body = build(HttpStatus.METHOD_NOT_ALLOWED, ErrorCodes.METHOD_NOT_ALLOWED,
                "HTTP method is not allowed for this endpoint", request);
        logExpected(ex, request);
        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) {
            ex.getSupportedHttpMethods().forEach(m -> headers.add(HttpHeaders.ALLOW, m.name()));
        }
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedMedia(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        ProblemDetail body = build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCodes.UNSUPPORTED_MEDIA,
                "Media type is not supported", request);
        logExpected(ex, request);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NoHandlerFoundException ex, HttpServletRequest request) {
        ProblemDetail body = build(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND,
                "Resource not found", request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail body = build(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN,
                "Access denied", request);
        logExpected(ex, request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    @ExceptionHandler({AuthenticationException.class, AuthenticationCredentialsNotFoundException.class})
    public ResponseEntity<ProblemDetail> handleAuthentication(Exception ex, HttpServletRequest request) {
        ProblemDetail body = build(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHENTICATED,
                "Authentication required", request);
        logExpected(ex, request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        ProblemDetail body = build(HttpStatus.CONFLICT, ErrorCodes.OPTIMISTIC_LOCK,
                "Resource was modified by another operation", request);
        logExpected(ex, request);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        // Never surface the SQL cause to the client.
        ProblemDetail body = build(HttpStatus.CONFLICT, ErrorCodes.CONFLICT,
                "Data integrity violation", request);
        log.warn("Data integrity violation on {} {} (correlation_id={})",
                request.getMethod(), request.getRequestURI(), MDC.get("correlation_id"));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        ProblemDetail body = build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR,
                "An unexpected error occurred", request);
        body.setProperty("error_id", errorId);
        log.error("Unhandled exception (error_id={}, correlation_id={}) on {} {}",
                errorId, MDC.get("correlation_id"),
                request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    private ProblemDetail build(HttpStatus status, String code, String title, HttpServletRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, LogSanitizer.safe(title));
        body.setType(URI.create(TYPE_BASE + code));
        body.setTitle(status.getReasonPhrase());
        body.setInstance(URI.create(request.getRequestURI()));
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("code", code);
        ext.put("timestamp", Instant.now().toString());
        String cid = MDC.get("correlation_id");
        if (cid != null) {
            ext.put("correlation_id", cid);
        }
        ext.forEach(body::setProperty);
        return body;
    }

    private void logExpected(Throwable ex, HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("Expected exception {} on {} {}: {}",
                    ex.getClass().getSimpleName(),
                    request.getMethod(), request.getRequestURI(),
                    LogSanitizer.safe(ex.getMessage()));
        }
    }
}
