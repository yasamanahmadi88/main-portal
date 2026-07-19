package com.company.portal.identity.repository;

import com.company.portal.identity.domain.LoginAttemptEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, UUID> {

    long countByEmailNormalizedAndAttemptedAtAfter(String emailNormalized, OffsetDateTime since);

    long countByIpAddressAndAttemptedAtAfter(String ipAddress, OffsetDateTime since);
}
