package com.company.portal.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Application-level authentication counters scraped by Prometheus / Grafana.
 * Metric names match the provisioned portal backend dashboard.
 */
@Component
public class AuthMetrics {

    private final Counter loginAttempts;
    private final Counter loginFailures;
    private final Counter loginSuccesses;
    private final Counter mfaChallenges;

    public AuthMetrics(MeterRegistry registry) {
        this.loginAttempts = Counter.builder("portal_authentication_login_attempts_total")
                .description("Total login attempts after CAPTCHA validation")
                .register(registry);
        this.loginFailures = Counter.builder("portal_authentication_login_failures_total")
                .description("Failed login attempts")
                .register(registry);
        this.loginSuccesses = Counter.builder("portal_authentication_login_success_total")
                .description("Successful password logins (pre-MFA or full)")
                .register(registry);
        this.mfaChallenges = Counter.builder("portal_authentication_mfa_challenges_total")
                .description("MFA challenges issued after valid password")
                .register(registry);
    }

    public void loginAttempt() {
        loginAttempts.increment();
    }

    public void loginFailure(String reason) {
        loginFailures.increment();
    }

    public void loginSuccess() {
        loginSuccesses.increment();
    }

    public void mfaChallenge() {
        mfaChallenges.increment();
    }
}
