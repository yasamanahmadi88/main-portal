package com.company.portal.accesscontrol.repository;

import com.company.portal.accesscontrol.domain.PermissionEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {

    Optional<PermissionEntity> findByCode(String code);

    @Query("SELECT p FROM PermissionEntity p WHERE p.code IN :codes")
    List<PermissionEntity> findByCodes(@Param("codes") Collection<String> codes);

    @Query("SELECT p FROM PermissionEntity p ORDER BY p.resource ASC, p.action ASC")
    List<PermissionEntity> findAllOrdered();
}
