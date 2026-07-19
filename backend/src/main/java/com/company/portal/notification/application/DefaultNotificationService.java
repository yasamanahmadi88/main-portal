package com.company.portal.notification.application;

import com.company.portal.notification.api.NotificationService;
import com.company.portal.notification.domain.OutboxEventEntity;
import com.company.portal.notification.repository.OutboxEventRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class DefaultNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationService.class);

    private final OutboxEventRepository outbox;
    private final ObjectMapper objectMapper;

    public DefaultNotificationService(OutboxEventRepository outbox, ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void enqueueEmail(String templateCode, String recipient, String locale,
                             Map<String, Object> variables) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("template", templateCode);
        payload.put("recipient", recipient);
        payload.put("locale", locale == null ? "en" : locale);
        payload.put("variables", variables == null ? Map.of() : variables);

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (RuntimeException e) {
            log.error("Failed to serialize notification payload for template {}", templateCode, e);
            return;
        }
        OutboxEventEntity event = new OutboxEventEntity(
                UUID.randomUUID(), "NOTIFICATION", recipient, "EMAIL_SEND", json);
        outbox.save(event);
    }
}
