package com.company.portal.identity.repository;

import com.company.portal.identity.domain.PasswordResetTokenEntity;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            UPDATE PasswordResetTokenEntity t
            SET t.consumedAt = :now
            WHERE t.userId = :userId AND t.consumedAt IS NULL
            """)
    int invalidateActiveForUser(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);
}
