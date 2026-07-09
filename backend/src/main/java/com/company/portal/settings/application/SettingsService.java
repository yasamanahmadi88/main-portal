package com.company.portal.settings.application;

import com.company.portal.audit.api.AuditContext;
import com.company.portal.audit.api.AuditOutcome;
import com.company.portal.audit.api.AuditService;
import com.company.portal.audit.api.AuditSeverityLevel;
import com.company.portal.settings.domain.SystemSettingEntity;
import com.company.portal.settings.repository.SystemSettingRepository;
import com.company.portal.shared.error.PortalException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Strongly-typed access to a small handful of well-known system settings.
 * Unknown keys are ignored on read (defaults returned) and rejected on
 * write.
 */
@Service
public class SettingsService {

    static final String DEFAULT_LANGUAGE = "default.language";
    static final String DEFAULT_THEME = "default.theme";
    static final String SESSION_TIMEOUT = "session.timeout_minutes";
    static final String MFA_REQUIRED_ADMINS = "mfa.required_for_admins";
    static final String AUDIT_RETENTION = "audit.retention_days";
    static final String LOGIN_RATE_LIMIT = "auth.login_rate_limit_per_minute";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final SystemSettingRepository repository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public SettingsService(SystemSettingRepository repository, ObjectMapper objectMapper,
                           AuditService auditService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    public record SettingsSnapshot(String defaultLanguage, String defaultTheme,
                                   int sessionTimeoutMinutes, boolean mfaRequiredForAdmins,
                                   int auditRetentionDays, Integer loginRateLimitPerMinute) { }

    @Transactional(readOnly = true)
    public SettingsSnapshot get() {
        return new SettingsSnapshot(
                readString(DEFAULT_LANGUAGE, "en-US"),
                readString(DEFAULT_THEME, "SYSTEM"),
                (int) readLong(SESSION_TIMEOUT, 30L),
                readBoolean(MFA_REQUIRED_ADMINS, true),
                (int) readLong(AUDIT_RETENTION, 365L),
                (int) readLong(LOGIN_RATE_LIMIT, 10L)
        );
    }

    @Transactional
    public SettingsSnapshot patch(UUID actorId, Map<String, Object> updates) {
        if (updates == null) return get();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = mapKey(entry.getKey());
            if (key == null) continue;
            writeValue(key, entry.getValue(), actorId);
        }
        auditService.append(AuditContext.builder()
                .eventType("SETTINGS_UPDATED")
                .category("SYSTEM")
                .severity(AuditSeverityLevel.NOTICE)
                .outcome(AuditOutcome.SUCCESS)
                .actorType("USER").actorId(actorId)
                .action("UPDATE_SETTINGS")
                .targetType("SETTINGS").targetId("system")
                .addPayload("keys", updates.keySet())
                .build());
        return get();
    }

    private static String mapKey(String apiKey) {
        return switch (apiKey) {
            case "defaultLanguage" -> DEFAULT_LANGUAGE;
            case "defaultTheme" -> DEFAULT_THEME;
            case "sessionTimeoutMinutes" -> SESSION_TIMEOUT;
            case "mfaRequiredForAdmins" -> MFA_REQUIRED_ADMINS;
            case "auditRetentionDays" -> AUDIT_RETENTION;
            case "loginRateLimitPerMinute" -> LOGIN_RATE_LIMIT;
            default -> null;
        };
    }

    private String readString(String key, String def) {
        return repository.findBySettingKey(key)
                .map(this::extractString)
                .filter(Objects::nonNull)
                .orElse(def);
    }

    private long readLong(String key, long def) {
        return repository.findBySettingKey(key)
                .map(this::extractLong)
                .orElse(def);
    }

    private boolean readBoolean(String key, boolean def) {
        return repository.findBySettingKey(key)
                .map(this::extractBoolean)
                .orElse(def);
    }

    private String extractString(SystemSettingEntity entity) {
        try {
            Map<String, Object> map = objectMapper.readValue(entity.getSettingValue(), MAP_TYPE);
            Object v = map.get("value");
            return v == null ? null : v.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private long extractLong(SystemSettingEntity entity) {
        try {
            Map<String, Object> map = objectMapper.readValue(entity.getSettingValue(), MAP_TYPE);
            Object v = map.get("value");
            return v instanceof Number n ? n.longValue() : Long.parseLong(v == null ? "0" : v.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    private boolean extractBoolean(SystemSettingEntity entity) {
        try {
            Map<String, Object> map = objectMapper.readValue(entity.getSettingValue(), MAP_TYPE);
            Object v = map.get("value");
            if (v instanceof Boolean b) return b;
            return v != null && Boolean.parseBoolean(v.toString());
        } catch (Exception e) {
            return false;
        }
    }

    private void writeValue(String key, Object value, UUID actorId) {
        if (value == null) {
            throw new PortalException.Validation("Setting value cannot be null for " + key);
        }
        SystemSettingEntity entity = repository.findBySettingKey(key)
                .orElseGet(() -> new SystemSettingEntity(UUID.randomUUID(), key));
        Map<String, Object> wrap = new LinkedHashMap<>();
        wrap.put("value", value);
        try {
            entity.setSettingValue(objectMapper.writeValueAsString(wrap));
        } catch (RuntimeException e) {
            throw new PortalException.Validation("Failed to serialize setting value");
        }
        entity.setSettingType(inferType(value));
        entity.setUpdatedBy(actorId);
        entity.setUpdatedAt(OffsetDateTime.now());
        repository.save(entity);
    }

    private static String inferType(Object value) {
        if (value instanceof Boolean) return "BOOLEAN";
        if (value instanceof Number) return "NUMBER";
        if (value instanceof Map<?, ?>) return "JSON";
        return "STRING";
    }
}
