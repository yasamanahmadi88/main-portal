package com.company.portal.identity.repository;

import com.company.portal.identity.domain.MfaRecoveryCodeEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCodeEntity, UUID> {

    @Query("""
            SELECT c FROM MfaRecoveryCodeEntity c
            WHERE c.userId = :userId AND c.usedAt IS NULL
            """)
    List<MfaRecoveryCodeEntity> findUnusedByUserId(@Param("userId") UUID userId);

    long countByUserIdAndUsedAtIsNull(UUID userId);

    @Modifying
    @Query("DELETE FROM MfaRecoveryCodeEntity c WHERE c.userId = :userId")
    int deleteAllForUser(@Param("userId") UUID userId);
}
