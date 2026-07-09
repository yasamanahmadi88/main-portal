package com.company.portal.identity.application;

import com.company.portal.identity.domain.UserEntity;
import com.company.portal.identity.domain.UserPreferencesEntity;
import com.company.portal.identity.repository.UserPreferencesRepository;
import com.company.portal.identity.repository.UserRepository;
import com.company.portal.shared.error.PortalException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Read/write of user preferences. Language and time-zone live on the
 * {@code users} table; theme and density live on {@code user_preferences}.
 * Extra keys are stashed in the {@code preferences_json} JSONB blob.
 */
@Service
public class PreferenceService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };
    private static final List<String> ALLOWED_LANGUAGES = List.of("en-US", "fa-IR");
    private static final List<String> ALLOWED_THEMES = List.of("LIGHT", "DARK", "SYSTEM");
    private static final List<String> ALLOWED_DENSITIES = List.of("COMFORTABLE", "COMPACT");

    private final UserPreferencesRepository preferences;
    private final UserRepository users;
    private final ObjectMapper objectMapper;

    public PreferenceService(UserPreferencesRepository preferences,
                             UserRepository users,
                             ObjectMapper objectMapper) {
        this.preferences = preferences;
        this.users = users;
        this.objectMapper = objectMapper;
    }

    public record PreferencesSnapshot(String language, String theme, String timezone, String density) { }

    @Transactional
    public PreferencesSnapshot getPreferences(UUID userId) {
        UserEntity user = mustLoadUser(userId);
        UserPreferencesEntity prefs = ensurePreferences(userId);
        Map<String, Object> extra = readJson(prefs.getPreferencesJson());
        String density = (String) extra.getOrDefault("density", "COMFORTABLE");
        return new PreferencesSnapshot(
                normalizeLanguage(user.getLocale()),
                prefs.getTheme(),
                user.getTimeZone(),
                density
        );
    }

    @Transactional
    public PreferencesSnapshot patchPreferences(UUID userId, String language, String theme,
                                                String timezone, String density) {
        UserEntity user = mustLoadUser(userId);
        UserPreferencesEntity prefs = ensurePreferences(userId);
        if (language != null) {
            validate(language, ALLOWED_LANGUAGES, "language");
            user.setLocale(language);
        }
        if (theme != null) {
            validate(theme, ALLOWED_THEMES, "theme");
            prefs.setTheme(theme);
        }
        if (timezone != null) {
            try {
                java.time.ZoneId.of(timezone);
            } catch (RuntimeException e) {
                throw new PortalException.Validation("Invalid timezone: " + timezone);
            }
            user.setTimeZone(timezone);
        }
        Map<String, Object> extra = new LinkedHashMap<>(readJson(prefs.getPreferencesJson()));
        if (density != null) {
            validate(density, ALLOWED_DENSITIES, "density");
            extra.put("density", density);
        }
        try {
            prefs.setPreferencesJson(objectMapper.writeValueAsString(extra));
        } catch (RuntimeException e) {
            throw new PortalException.Validation("Failed to persist preference blob");
        }
        prefs.setUpdatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        return new PreferencesSnapshot(
                normalizeLanguage(user.getLocale()),
                prefs.getTheme(),
                user.getTimeZone(),
                (String) extra.getOrDefault("density", "COMFORTABLE"));
    }

    UserPreferencesEntity ensurePreferences(UUID userId) {
        return preferences.findByUserId(userId).orElseGet(() -> {
            UserPreferencesEntity fresh = new UserPreferencesEntity(UUID.randomUUID(), userId);
            return preferences.save(fresh);
        });
    }

    private UserEntity mustLoadUser(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new PortalException.NotFound("User not found"));
    }

    private Map<String, Object> readJson(String blob) {
        if (blob == null || blob.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(blob, MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static void validate(String candidate, List<String> allowed, String field) {
        if (!allowed.contains(candidate)) {
            throw new PortalException.Validation("Invalid " + field + ": " + candidate);
        }
    }

    private static String normalizeLanguage(String locale) {
        if (locale == null || locale.isBlank()) return "en-US";
        Locale l = Locale.forLanguageTag(locale.replace('_', '-'));
        String tag = l.toLanguageTag();
        return tag.isBlank() ? "en-US" : tag;
    }
}
