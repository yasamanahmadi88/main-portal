package com.company.portal.identity.repository;

import com.company.portal.identity.domain.UserEntity;
import com.company.portal.identity.domain.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmailNormalized(String emailNormalized);

    boolean existsByEmailNormalized(String emailNormalized);

    @Query("""
            SELECT u FROM UserEntity u
            WHERE u.deletedAt IS NULL
              AND (:q IS NULL OR LOWER(u.emailNormalized) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:status IS NULL OR u.status = :status)
            """)
    Page<UserEntity> search(@Param("q") String q,
                            @Param("status") UserStatus status,
                            Pageable pageable);

    long countByStatusAndDeletedAtIsNull(UserStatus status);
}
