package com.company.portal.settings.repository;

import com.company.portal.settings.domain.SystemSettingEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSettingEntity, UUID> {

    Optional<SystemSettingEntity> findBySettingKey(String key);
}
