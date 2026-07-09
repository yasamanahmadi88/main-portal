package com.company.portal.notification.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

/**
 * Loads email templates from the classpath and renders them with a tiny
 * {@code {{variable}}} placeholder syntax. Templates live under
 * {@code classpath:templates/email/<locale>/<template-code>.html} with an
 * optional {@code .subject.txt} sibling for the subject line.
 *
 * <p>Deliberately minimal — no partials, conditionals, or user code — to keep
 * the surface area small and injection-proof.</p>
 */
@Component
public class EmailTemplateRegistry {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateRegistry.class);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*\\}\\}");
    private static final List<String> KNOWN_LOCALES = List.of("en", "fa");
    private static final String DEFAULT_LOCALE = "en";

    private static final Map<String, Fallback> FALLBACKS = Map.of(
            "password_reset", new Fallback("Reset your password",
                    "Hello,\n\nUse the following link to reset your password:\n{{resetUrl}}\n\nThis link expires at {{expiresAt}}."),
            "password_changed", new Fallback("Your password was changed",
                    "Hello,\n\nYour password was changed at {{changedAt}}. If this wasn't you, contact support immediately."),
            "mfa_enabled", new Fallback("Multi-factor authentication enabled",
                    "Hello,\n\nMFA was enabled on your account at {{enabledAt}}."),
            "mfa_disabled", new Fallback("Multi-factor authentication disabled",
                    "Hello,\n\nMFA was disabled on your account at {{disabledAt}}."),
            "suspicious_login", new Fallback("Unusual sign-in activity",
                    "Hello,\n\nWe detected a sign-in from {{ipAddress}} at {{occurredAt}}. If this wasn't you, please change your password."),
            "invitation", new Fallback("You're invited to Portal",
                    "Hello,\n\nYou have been invited. Complete your profile at {{portalUrl}}.")
    );

    public Rendered render(String templateCode, String locale, Map<String, Object> variables) {
        String effectiveLocale = KNOWN_LOCALES.contains(locale) ? locale : DEFAULT_LOCALE;
        Fallback fallback = FALLBACKS.getOrDefault(templateCode,
                new Fallback("Portal notification", "{{message}}"));

        String subject = readOrFallback("templates/email/" + effectiveLocale + "/"
                + templateCode + ".subject.txt", fallback.subject);
        String body = readOrFallback("templates/email/" + effectiveLocale + "/"
                + templateCode + ".txt", fallback.body);
        return new Rendered(replace(subject, variables), replace(body, variables));
    }

    private static String replace(String template, Map<String, Object> vars) {
        if (template == null) return "";
        Map<String, Object> safe = vars == null ? Map.of() : vars;
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            Object v = safe.get(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(v == null ? "" : v.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String readOrFallback(String classpath, String fallback) {
        try {
            ClassPathResource res = new ClassPathResource(classpath);
            if (!res.exists()) {
                return fallback;
            }
            byte[] bytes = FileCopyUtils.copyToByteArray(res.getInputStream());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.debug("Falling back to inline template for {}: {}", classpath, e.getMessage());
            return fallback;
        }
    }

    public Map<String, Fallback> defaults() {
        return new LinkedHashMap<>(FALLBACKS);
    }

    public record Rendered(String subject, String body) { }

    public record Fallback(String subject, String body) { }
}
