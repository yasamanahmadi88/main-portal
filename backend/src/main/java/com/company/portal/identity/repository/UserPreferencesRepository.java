package com.company.portal.identity.repository;

import com.company.portal.identity.domain.UserPreferencesEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferencesRepository extends JpaRepository<UserPreferencesEntity, UUID> {

    Optional<UserPreferencesEntity> findByUserId(UUID userId);
}
