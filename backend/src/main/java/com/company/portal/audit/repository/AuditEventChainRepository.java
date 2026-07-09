package com.company.portal.audit.repository;

import com.company.portal.audit.domain.AuditEventChainEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventChainRepository extends JpaRepository<AuditEventChainEntity, String> {

    /**
     * Fetches the chain tail with a database row lock so concurrent appends
     * cannot race on the sequence number.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM AuditEventChainEntity c WHERE c.stream = :stream")
    Optional<AuditEventChainEntity> lockForUpdate(@Param("stream") String stream);
}
