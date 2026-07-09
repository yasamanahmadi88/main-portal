package com.company.portal.accesscontrol.repository;

import com.company.portal.accesscontrol.domain.RoleEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT r FROM RoleEntity r WHERE r.code IN :codes")
    List<RoleEntity> findByCodes(@Param("codes") Collection<String> codes);

    @Query("SELECT r FROM RoleEntity r ORDER BY r.code ASC")
    List<RoleEntity> findAllOrdered();
}
