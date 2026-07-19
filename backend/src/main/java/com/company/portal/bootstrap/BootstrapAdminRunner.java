package com.company.portal.bootstrap;

import com.company.portal.audit.api.AuditContext;
import com.company.portal.audit.api.AuditOutcome;
import com.company.portal.audit.api.AuditService;
import com.company.portal.audit.api.AuditSeverityLevel;
import com.company.portal.shared.config.PortalProperties;
import com.company.portal.shared.logging.LogSanitizer;
import com.company.portal.shared.security.PortalRoles;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Provisions the initial SUPER_ADMIN user when {@code portal.bootstrap.enabled}
 * is true, both {@code admin-email} and {@code admin-password} are configured,
 * and the {@code users} table is empty.
 *
 * <p>Idempotent: once any user exists the runner logs a single line and does
 * nothing. Never logs the plaintext password. Emits an INFO log describing
 * that a bootstrap admin was created so the deployer can immediately rotate
 * the seed credential.</p>
 */
@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final PortalProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final AuditService auditService;

    public BootstrapAdminRunner(PortalProperties properties,
                                PasswordEncoder passwordEncoder,
                                DataSource dataSource,
                                PlatformTransactionManager transactionManager,
                                AuditService auditService) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = new JdbcTemplate(dataSource);
        this.tx = new TransactionTemplate(transactionManager);
        this.auditService = auditService;
    }

    @Override
    public void run(ApplicationArguments args) {
        PortalProperties.Bootstrap cfg = properties.getBootstrap();
        if (!cfg.isConfigured()) {
            log.debug("Bootstrap admin runner skipped: not enabled or credentials missing.");
            return;
        }

        Boolean anyUsersExist = tx.execute(status -> {
            Long existing = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
            return existing != null && existing > 0L;
        });
        if (Boolean.TRUE.equals(anyUsersExist)) {
            log.info("Bootstrap admin skipped: {} user(s) already present.",
                    jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));
            return;
        }

        UUID adminId = UUID.randomUUID();
        String emailNormalized = properties.normalizeEmail(cfg.getAdminEmail());
        String encoded = passwordEncoder.encode(cfg.getAdminPassword());
        OffsetDateTime now = OffsetDateTime.now();

        Boolean created = tx.execute(status -> {
            jdbc.update("""
                    INSERT INTO users (
                        id, email, email_normalized, email_verified, password_hash,
                        display_name, status, mfa_enabled, mfa_enforced,
                        created_at, updated_at, last_password_changed_at, version
                    ) VALUES (?, ?, ?, TRUE, ?, ?, 'ACTIVE', FALSE, TRUE, ?, ?, ?, 0)
                    """,
                    adminId, cfg.getAdminEmail(), emailNormalized, encoded,
                    cfg.getAdminDisplayName(), now, now, now);

            jdbc.update("""
                    INSERT INTO user_roles (user_id, role_id, assigned_at)
                    SELECT ?, r.id, ? FROM roles r WHERE r.code = ?
                    """, adminId, now, PortalRoles.SUPER_ADMIN);

            jdbc.update("""
                    INSERT INTO user_preferences (user_id) VALUES (?)
                    """, adminId);
            return Boolean.TRUE;
        });

        if (Boolean.TRUE.equals(created)) {
            log.warn("""
                    Bootstrap admin created: id={}, email={}, role={}. \
                    ROTATE THIS PASSWORD IMMEDIATELY AND DISABLE portal.bootstrap.enabled.""",
                    adminId, LogSanitizer.maskEmail(cfg.getAdminEmail()), PortalRoles.SUPER_ADMIN);
            try {
                auditService.append(AuditContext.builder()
                        .eventType("BOOTSTRAP_ADMIN_CREATED")
                        .category("BOOTSTRAP")
                        .severity(AuditSeverityLevel.CRITICAL)
                        .outcome(AuditOutcome.SUCCESS)
                        .actorType("SYSTEM")
                        .targetType("USER")
                        .targetId(adminId.toString())
                        .targetDisplay(LogSanitizer.maskEmail(cfg.getAdminEmail()))
                        .action("BOOTSTRAP_ADMIN_CREATED")
                        .addPayload("role", PortalRoles.SUPER_ADMIN)
                        .build());
            } catch (RuntimeException auditFailure) {
                // Admin row is already committed; do not fail process startup if audit
                // persistence has a transient mapping/DB issue. Operators still see the WARN.
                log.error("Bootstrap admin created but audit append failed: {}",
                        auditFailure.toString());
            }
        }
    }
}
