package com.company.portal.shared.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Root of the {@code portal.*} configuration namespace. Bound once at startup
 * and injected throughout the platform to avoid scattering {@code @Value}.
 */
@ConfigurationProperties(prefix = "portal")
public class PortalProperties {

    private String publicBaseUrl = "http://localhost:8080";
    private Session session = new Session();
    private Mfa mfa = new Mfa();
    private Bootstrap bootstrap = new Bootstrap();
    private RateLimit rateLimit = new RateLimit();
    private Security security = new Security();
    private Password password = new Password();
    private Audit audit = new Audit();

    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }

    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }

    public Mfa getMfa() { return mfa; }
    public void setMfa(Mfa mfa) { this.mfa = mfa; }

    public Bootstrap getBootstrap() { return bootstrap; }
    public void setBootstrap(Bootstrap bootstrap) { this.bootstrap = bootstrap; }

    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }

    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }

    public Password getPassword() { return password; }
    public void setPassword(Password password) { this.password = password; }

    public Audit getAudit() { return audit; }
    public void setAudit(Audit audit) { this.audit = audit; }

    public static class Session {
        private Duration idleTimeout = Duration.ofMinutes(30);
        private Duration absoluteTimeout = Duration.ofHours(12);
        private Duration renewalWindow = Duration.ofMinutes(5);
        private int maxConcurrentSessions = 5;
        private Cookie cookie = new Cookie();

        public Duration getIdleTimeout() { return idleTimeout; }
        public void setIdleTimeout(Duration idleTimeout) { this.idleTimeout = idleTimeout; }
        public Duration getAbsoluteTimeout() { return absoluteTimeout; }
        public void setAbsoluteTimeout(Duration absoluteTimeout) { this.absoluteTimeout = absoluteTimeout; }
        public Duration getRenewalWindow() { return renewalWindow; }
        public void setRenewalWindow(Duration renewalWindow) { this.renewalWindow = renewalWindow; }
        public int getMaxConcurrentSessions() { return maxConcurrentSessions; }
        public void setMaxConcurrentSessions(int maxConcurrentSessions) { this.maxConcurrentSessions = maxConcurrentSessions; }
        public Cookie getCookie() { return cookie; }
        public void setCookie(Cookie cookie) { this.cookie = cookie; }
    }

    public static class Cookie {
        private String name = "PORTAL_SESSION";
        private boolean secure = false;
        private boolean httpOnly = true;
        private SameSite sameSite = SameSite.LAX;
        private String path = "/";
        private boolean hostPrefix = false;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isSecure() { return secure; }
        public void setSecure(boolean secure) { this.secure = secure; }
        public boolean isHttpOnly() { return httpOnly; }
        public void setHttpOnly(boolean httpOnly) { this.httpOnly = httpOnly; }
        public SameSite getSameSite() { return sameSite; }
        public void setSameSite(SameSite sameSite) { this.sameSite = sameSite; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public boolean isHostPrefix() { return hostPrefix; }
        public void setHostPrefix(boolean hostPrefix) { this.hostPrefix = hostPrefix; }

        public String effectiveName() {
            if (hostPrefix && !name.startsWith("__Host-")) {
                return "__Host-" + name;
            }
            return name;
        }
    }

    public enum SameSite {
        NONE, LAX, STRICT;

        public String attributeValue() {
            return switch (this) {
                case NONE -> "None";
                case LAX -> "Lax";
                case STRICT -> "Strict";
            };
        }
    }

    public static class Mfa {
        private Totp totp = new Totp();
        private RecoveryCodes recoveryCodes = new RecoveryCodes();
        private Encryption encryption = new Encryption();

        public Totp getTotp() { return totp; }
        public void setTotp(Totp totp) { this.totp = totp; }
        public RecoveryCodes getRecoveryCodes() { return recoveryCodes; }
        public void setRecoveryCodes(RecoveryCodes recoveryCodes) { this.recoveryCodes = recoveryCodes; }
        public Encryption getEncryption() { return encryption; }
        public void setEncryption(Encryption encryption) { this.encryption = encryption; }
    }

    public static class Totp {
        private String issuer = "Portal";
        private int timeStepSeconds = 30;
        private int digits = 6;
        private int window = 1;

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public int getTimeStepSeconds() { return timeStepSeconds; }
        public void setTimeStepSeconds(int timeStepSeconds) { this.timeStepSeconds = timeStepSeconds; }
        public int getDigits() { return digits; }
        public void setDigits(int digits) { this.digits = digits; }
        public int getWindow() { return window; }
        public void setWindow(int window) { this.window = window; }
    }

    public static class RecoveryCodes {
        private int count = 10;
        private int length = 10;

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }
    }

    public static class Encryption {
        private String activeKeyId;
        private Map<String, String> keys = new LinkedHashMap<>();

        public String getActiveKeyId() { return activeKeyId; }
        public void setActiveKeyId(String activeKeyId) { this.activeKeyId = activeKeyId; }
        public Map<String, String> getKeys() { return keys; }
        public void setKeys(Map<String, String> keys) { this.keys = keys; }
    }

    public static class Bootstrap {
        private boolean enabled = false;
        private String adminEmail;
        private String adminPassword;
        private String adminDisplayName = "Portal Administrator";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getAdminEmail() { return adminEmail; }
        public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
        public String getAdminPassword() { return adminPassword; }
        public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
        public String getAdminDisplayName() { return adminDisplayName; }
        public void setAdminDisplayName(String adminDisplayName) { this.adminDisplayName = adminDisplayName; }

        public boolean isConfigured() {
            return enabled
                    && adminEmail != null && !adminEmail.isBlank()
                    && adminPassword != null && !adminPassword.isBlank();
        }
    }

    public static class RateLimit {
        private Bucket login = new Bucket(10, 10, Duration.ofMinutes(1));
        private Bucket passwordReset = new Bucket(5, 5, Duration.ofMinutes(15));
        private Bucket mfaVerify = new Bucket(10, 10, Duration.ofMinutes(5));
        private Bucket globalApi = new Bucket(600, 600, Duration.ofMinutes(1));

        public Bucket getLogin() { return login; }
        public void setLogin(Bucket login) { this.login = login; }
        public Bucket getPasswordReset() { return passwordReset; }
        public void setPasswordReset(Bucket passwordReset) { this.passwordReset = passwordReset; }
        public Bucket getMfaVerify() { return mfaVerify; }
        public void setMfaVerify(Bucket mfaVerify) { this.mfaVerify = mfaVerify; }
        public Bucket getGlobalApi() { return globalApi; }
        public void setGlobalApi(Bucket globalApi) { this.globalApi = globalApi; }
    }

    public static class Bucket {
        private long capacity;
        private long refillTokens;
        private Duration refillPeriod;

        public Bucket() { }
        public Bucket(long capacity, long refillTokens, Duration refillPeriod) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillPeriod = refillPeriod;
        }
        public long getCapacity() { return capacity; }
        public void setCapacity(long capacity) { this.capacity = capacity; }
        public long getRefillTokens() { return refillTokens; }
        public void setRefillTokens(long refillTokens) { this.refillTokens = refillTokens; }
        public Duration getRefillPeriod() { return refillPeriod; }
        public void setRefillPeriod(Duration refillPeriod) { this.refillPeriod = refillPeriod; }
    }

    public static class Security {
        private String csp = "default-src 'self'";
        private String referrerPolicy = "strict-origin-when-cross-origin";
        private String permissionsPolicy = "camera=(), microphone=(), geolocation=()";
        private long hstsMaxAgeSeconds = 31_536_000L;
        private List<String> allowedOrigins = new ArrayList<>();

        public String getCsp() { return csp; }
        public void setCsp(String csp) { this.csp = csp; }
        public String getReferrerPolicy() { return referrerPolicy; }
        public void setReferrerPolicy(String referrerPolicy) { this.referrerPolicy = referrerPolicy; }
        public String getPermissionsPolicy() { return permissionsPolicy; }
        public void setPermissionsPolicy(String permissionsPolicy) { this.permissionsPolicy = permissionsPolicy; }
        public long getHstsMaxAgeSeconds() { return hstsMaxAgeSeconds; }
        public void setHstsMaxAgeSeconds(long hstsMaxAgeSeconds) { this.hstsMaxAgeSeconds = hstsMaxAgeSeconds; }
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class Password {
        private Argon2 argon2 = new Argon2();

        public Argon2 getArgon2() { return argon2; }
        public void setArgon2(Argon2 argon2) { this.argon2 = argon2; }
    }

    public static class Argon2 {
        private int memoryKib = 65_536;
        private int iterations = 3;
        private int parallelism = 1;
        private int saltLength = 16;
        private int hashLength = 32;

        public int getMemoryKib() { return memoryKib; }
        public void setMemoryKib(int memoryKib) { this.memoryKib = memoryKib; }
        public int getIterations() { return iterations; }
        public void setIterations(int iterations) { this.iterations = iterations; }
        public int getParallelism() { return parallelism; }
        public void setParallelism(int parallelism) { this.parallelism = parallelism; }
        public int getSaltLength() { return saltLength; }
        public void setSaltLength(int saltLength) { this.saltLength = saltLength; }
        public int getHashLength() { return hashLength; }
        public void setHashLength(int hashLength) { this.hashLength = hashLength; }
    }

    public static class Audit {
        private boolean hashChainEnabled = true;
        private String hashAlgorithm = "SHA-256";

        public boolean isHashChainEnabled() { return hashChainEnabled; }
        public void setHashChainEnabled(boolean hashChainEnabled) { this.hashChainEnabled = hashChainEnabled; }
        public String getHashAlgorithm() { return hashAlgorithm; }
        public void setHashAlgorithm(String hashAlgorithm) { this.hashAlgorithm = hashAlgorithm; }
    }

    public String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
