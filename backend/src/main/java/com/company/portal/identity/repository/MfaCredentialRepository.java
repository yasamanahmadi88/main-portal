package com.company.portal.identity.repository;

import com.company.portal.identity.domain.MfaCredentialEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MfaCredentialRepository extends JpaRepository<MfaCredentialEntity, UUID> {

    List<MfaCredentialEntity> findByUserId(UUID userId);

    Optional<MfaCredentialEntity> findByUserIdAndType(UUID userId, String type);

    Optional<MfaCredentialEntity> findByUserIdAndTypeAndActivatedTrue(UUID userId, String type);

    @Modifying
    @Query("DELETE FROM MfaCredentialEntity m WHERE m.userId = :userId")
    int deleteAllForUser(@Param("userId") UUID userId);
}
