package com.company.portal.identity.repository;

import com.company.portal.identity.domain.UserSessionMetadataEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionMetadataRepository extends JpaRepository<UserSessionMetadataEntity, UUID> {

    Optional<UserSessionMetadataEntity> findBySessionId(String sessionId);

    @Query("""
            SELECT s FROM UserSessionMetadataEntity s
            WHERE s.userId = :userId AND s.revokedAt IS NULL
            ORDER BY s.lastSeenAt DESC
            """)
    List<UserSessionMetadataEntity> findActiveByUserId(@Param("userId") UUID userId);

    long countByRevokedAtIsNullAndLastSeenAtAfter(OffsetDateTime since);

    @Modifying
    @Query("""
            UPDATE UserSessionMetadataEntity s
            SET s.revokedAt = :now, s.revokeReason = :reason
            WHERE s.userId = :userId AND s.revokedAt IS NULL
              AND (:exceptSessionId IS NULL OR s.sessionId <> :exceptSessionId)
            """)
    int revokeAllForUser(@Param("userId") UUID userId,
                         @Param("exceptSessionId") String exceptSessionId,
                         @Param("reason") String reason,
                         @Param("now") OffsetDateTime now);
}
