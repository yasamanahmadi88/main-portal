package com.company.portal.audit.api;

import java.util.UUID;

/**
 * Public API of the audit module. Callers use {@link AuditContext#builder()}
 * to construct the payload and pass it here; the implementation transactionally
 * appends the event under a row lock so the hash chain remains monotonic.
 *
 * <p>The method returns the assigned event id so callers can correlate log
 * lines. If the append fails the exception propagates — callers who consider
 * the audit trail critical must NOT swallow it.</p>
 */
public interface AuditService {

    UUID append(AuditContext context);
}
