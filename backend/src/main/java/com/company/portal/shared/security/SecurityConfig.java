package com.company.portal.shared.security;

import com.company.portal.shared.config.PortalProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Minimal Spring Security baseline for the portal. Feature modules (identity,
 * MFA) attach additional {@link SecurityFilterChain}s and providers.
 *
 * <p>Rules:
 * <ul>
 *   <li>Public: {@code /actuator/health}, {@code /actuator/info},
 *       {@code /api/v1/auth/**}, and (when enabled) Swagger UI.</li>
 *   <li>CSRF: enabled with a {@link CookieCsrfTokenRepository} so Angular's
 *       XSRF-TOKEN interceptor works out of the box.</li>
 *   <li>Sessions: server-side via Spring Session Redis; policy IF_REQUIRED.</li>
 *   <li>Everything else requires authentication (fail-closed by default).</li>
 * </ul>
 * </p>
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/api/v1/auth/**",
            "/error"
    };

    private static final String[] SWAGGER_ENDPOINTS = {
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
    };

    private final PortalProperties properties;
    private final boolean swaggerEnabled;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(PortalProperties properties,
                          @Value("${springdoc.swagger-ui.enabled:false}") boolean swaggerEnabled,
                          AuthenticationEntryPoint authenticationEntryPoint,
                          AccessDeniedHandler accessDeniedHandler) {
        this.properties = properties;
        this.swaggerEnabled = swaggerEnabled;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        boolean secureCookie = properties.getSession().getCookie().isSecure();
        String sameSite = properties.getSession().getCookie().getSameSite().attributeValue();
        csrfRepo.setCookieCustomizer(cookie -> cookie
                .path("/")
                .httpOnly(false)
                .secure(secureCookie)
                .sameSite(sameSite));

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepo)
                        .ignoringRequestMatchers("/actuator/**"))
                .sessionManagement(this::configureSessions)
                .headers(this::configureHeaders)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(reg -> {
                    reg.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    reg.requestMatchers(PUBLIC_ENDPOINTS).permitAll();
                    if (swaggerEnabled) {
                        reg.requestMatchers(SWAGGER_ENDPOINTS).permitAll();
                    }
                    reg.requestMatchers("/actuator/**").hasAuthority(PortalPermission.SETTINGS_READ.authority());
                    reg.anyRequest().authenticated();
                })
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }

    private void configureSessions(SessionManagementConfigurer<HttpSecurity> sm) {
        sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::migrateSession)
                .maximumSessions(properties.getSession().getMaxConcurrentSessions())
                .maxSessionsPreventsLogin(false);
    }

    private void configureHeaders(HeadersConfigurer<HttpSecurity> headers) {
        headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp -> csp.policyDirectives(properties.getSecurity().getCsp()))
                .permissionsPolicyHeader(p -> p.policy(properties.getSecurity().getPermissionsPolicy()))
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(properties.getSecurity().getHstsMaxAgeSeconds()))
                .cacheControl(Customizer.withDefaults());
    }

    /**
     * Argon2id password encoder with runtime-tunable parameters. Wrapped in a
     * {@link DelegatingPasswordEncoder} keyed by {@code argon2} so encoded
     * hashes carry a self-describing prefix (e.g. {@code {argon2}$argon2id$…}).
     * Legacy hashes prefixed with other identifiers can be added to
     * {@code encoders} later without invalidating existing users.
     */
    @Bean
    public PasswordEncoder passwordEncoder(PortalProperties properties) {
        PortalProperties.Argon2 a = properties.getPassword().getArgon2();
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(
                a.getSaltLength(), a.getHashLength(),
                a.getParallelism(), a.getMemoryKib(), a.getIterations());
        Map<String, PasswordEncoder> encoders = Map.of("argon2", argon2);
        DelegatingPasswordEncoder delegating = new DelegatingPasswordEncoder("argon2", encoders);
        delegating.setDefaultPasswordEncoderForMatches(argon2);
        return delegating;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origins = properties.getSecurity().getAllowedOrigins();
        if (origins != null && !origins.isEmpty()) {
            cfg.setAllowedOrigins(origins);
        }
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-ID",
                "X-Request-ID", "X-XSRF-TOKEN"));
        cfg.setExposedHeaders(List.of("X-Correlation-ID", "X-Request-ID"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(Duration.ofMinutes(30));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
