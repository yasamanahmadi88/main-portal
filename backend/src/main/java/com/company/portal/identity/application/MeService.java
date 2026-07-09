package com.company.portal.identity.application;

import com.company.portal.identity.domain.UserEntity;
import com.company.portal.identity.repository.UserRepository;
import com.company.portal.identity.web.UserDto;
import com.company.portal.shared.error.PortalException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service profile use-cases for the currently authenticated user. Keeps
 * the {@code MeController} free of direct JPA repository access.
 */
@Service
public class MeService {

    private final UserRepository users;
    private final UserMapper userMapper;

    public MeService(UserRepository users, UserMapper userMapper) {
        this.users = users;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public UserEntity currentEntity(UUID userId) {
        return users.findById(userId).orElseThrow(
                () -> new PortalException.NotFound("Current user not found"));
    }

    @Transactional(readOnly = true)
    public UserDto currentProfile(UUID userId) {
        return userMapper.toDto(currentEntity(userId));
    }

    @Transactional
    public UserDto updateProfile(UUID userId, String displayName, String email) {
        UserEntity user = currentEntity(userId);
        if (displayName != null) user.setDisplayName(displayName);
        if (email != null) {
            String normalized = email.trim().toLowerCase();
            if (!normalized.equals(user.getEmailNormalized())
                    && users.existsByEmailNormalized(normalized)) {
                throw new PortalException.Conflict("Email already registered");
            }
            user.setEmail(email);
            user.setEmailNormalized(normalized);
            user.setEmailVerified(false);
        }
        user.setUpdatedAt(OffsetDateTime.now());
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public Optional<String> usernameForUserId(UUID userId) {
        return users.findById(userId).map(UserEntity::getEmailNormalized);
    }
}
