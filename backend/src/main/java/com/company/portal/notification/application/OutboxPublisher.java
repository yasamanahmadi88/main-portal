package com.company.portal.notification.application;

import com.company.portal.notification.domain.OutboxEventEntity;
import com.company.portal.notification.repository.OutboxEventRepository;
import com.company.portal.shared.logging.LogSanitizer;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads {@code outbox_events} whose {@code next_attempt_at} has arrived and
 * attempts delivery. Backs off exponentially on failure and marks events as
 * {@code DEAD_LETTER} once {@code max_attempts} is exhausted.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final OutboxEventRepository outbox;
    private final EmailTemplateRegistry templates;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final String fromAddress;
    private final boolean mailEnabled;

    public OutboxPublisher(OutboxEventRepository outbox,
                           EmailTemplateRegistry templates,
                           JavaMailSender mailSender,
                           ObjectMapper objectMapper,
                           @Value("${portal.notification.mail-from:no-reply@portal.local}") String fromAddress,
                           @Value("${portal.notification.mail-enabled:true}") boolean mailEnabled) {
        this.outbox = outbox;
        this.templates = templates;
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
        this.fromAddress = fromAddress;
        this.mailEnabled = mailEnabled;
    }

    @Scheduled(fixedDelayString = "${portal.notification.poll-interval-ms:5000}")
    @Transactional
    public void processDueEvents() {
        List<OutboxEventEntity> due = outbox.findDue(OffsetDateTime.now(), PageRequest.of(0, 25));
        for (OutboxEventEntity event : due) {
            deliver(event);
        }
    }

    private void deliver(OutboxEventEntity event) {
        event.setAttemptCount(event.getAttemptCount() + 1);
        event.setUpdatedAt(OffsetDateTime.now());
        try {
            if ("EMAIL_SEND".equals(event.getEventType())) {
                sendEmail(event);
            }
            event.setStatus("PUBLISHED");
            event.setPublishedAt(OffsetDateTime.now());
            event.setLastError(null);
        } catch (Exception e) {
            String reason = LogSanitizer.safe(e.getMessage());
            log.warn("Outbox delivery failed (id={}, attempt={}): {}",
                    event.getId(), event.getAttemptCount(), reason);
            event.setLastError(reason);
            if (event.getAttemptCount() >= event.getMaxAttempts()) {
                event.setStatus("DEAD_LETTER");
            } else {
                event.setStatus("PENDING");
                Duration backoff = Duration.ofSeconds(
                        Math.min(600, (long) Math.pow(2, event.getAttemptCount())));
                event.setNextAttemptAt(OffsetDateTime.now().plus(backoff));
            }
        }
        outbox.save(event);
    }

    private void sendEmail(OutboxEventEntity event) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(event.getPayloadJson(), MAP_TYPE);
        String recipient = (String) payload.get("recipient");
        String template = (String) payload.get("template");
        String locale = (String) payload.getOrDefault("locale", "en");
        Object variables = payload.get("variables");
        Map<String, Object> vars = variables instanceof Map<?, ?> m
                ? castMap(m)
                : Map.of();

        EmailTemplateRegistry.Rendered rendered = templates.render(template, locale, vars);
        if (!mailEnabled) {
            log.info("Mail delivery disabled; would send to={} subject=\"{}\"",
                    LogSanitizer.maskEmail(recipient),
                    LogSanitizer.safe(rendered.subject()));
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(rendered.subject());
        message.setText(rendered.body());
        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new Exception("Mail send failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> raw) {
        return (Map<String, Object>) raw;
    }
}
