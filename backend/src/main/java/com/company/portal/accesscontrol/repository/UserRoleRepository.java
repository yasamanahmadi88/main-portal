package com.company.portal.accesscontrol.repository;

import com.company.portal.accesscontrol.domain.UserRoleEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleEntity.UserRoleId> {

    @Query("SELECT ur FROM UserRoleEntity ur WHERE ur.id.userId = :userId")
    List<UserRoleEntity> findByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM UserRoleEntity ur WHERE ur.id.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);
}
