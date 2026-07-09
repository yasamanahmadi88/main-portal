package com.company.portal.notification.api;

import java.util.Map;

/**
 * Public API for the notification module. Callers hand off a template code and
 * variables; the implementation writes an outbox row so delivery happens
 * outside the caller's transaction.
 */
public interface NotificationService {

    /**
     * Enqueue an email delivery. Returns quickly and does not attempt to send.
     */
    void enqueueEmail(String templateCode, String recipient, String locale, Map<String, Object> variables);
}
