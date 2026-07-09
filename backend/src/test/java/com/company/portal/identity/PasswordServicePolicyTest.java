package com.company.portal.identity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.portal.audit.api.AuditService;
import com.company.portal.identity.application.PasswordService;
import com.company.portal.identity.domain.UserEntity;
import com.company.portal.identity.repository.PasswordResetTokenRepository;
import com.company.portal.identity.repository.UserRepository;
import com.company.portal.notification.api.NotificationService;
import com.company.portal.shared.config.PortalProperties;
import com.company.portal.shared.error.PortalException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password strength policy assertions. The service must accept only
 * passwords that meet the length + character-class rules, and it must never
 * enumerate account presence via {@code requestPasswordReset}.
 */
class PasswordServicePolicyTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final AuditService audit = mock(AuditService.class);
    private final PasswordEncoder encoder = new DelegatingPasswordEncoder(
            "argon2",
            java.util.Map.of("argon2", new Argon2PasswordEncoder(16, 32, 1, 65_536, 3)));
    private final PortalProperties properties = new PortalProperties();

    private final PasswordService service = new PasswordService(
            users, tokens, encoder, notifications, audit, properties);

    @Test
    void rejectsShortPasswordOnChange() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity(userId, "u@example.com", "u@example.com", "U");
        user.setPasswordHash(encoder.encode("Correct-Horse-Battery-42!"));
        when(users.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changePassword(userId, "Correct-Horse-Battery-42!", "short"))
                .isInstanceOf(PortalException.Validation.class);
    }

    @Test
    void rejectsWeakClassPassword() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity(userId, "u@example.com", "u@example.com", "U");
        user.setPasswordHash(encoder.encode("Correct-Horse-Battery-42!"));
        when(users.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changePassword(userId,
                "Correct-Horse-Battery-42!", "alllowercaseletters"))
                .isInstanceOf(PortalException.Validation.class);
    }

    @Test
    void forgotPasswordIsSilentForUnknownEmail() {
        when(users.findByEmailNormalized(any())).thenReturn(Optional.empty());
        assertThatCode(() -> service.requestPasswordReset("ghost@example.com", "1.2.3.4", "curl"))
                .doesNotThrowAnyException();
    }
}
