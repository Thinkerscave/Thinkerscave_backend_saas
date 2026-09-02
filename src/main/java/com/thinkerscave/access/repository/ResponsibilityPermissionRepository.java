package com.thinkerscave.access.repository;

import com.thinkerscave.access.entity.ResponsibilityPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResponsibilityPermissionRepository extends JpaRepository<ResponsibilityPermission, Long> {

    List<ResponsibilityPermission> findByResponsibility_ResponsibilityIdAndOrganization_Id(
            Long responsibilityId, Long organizationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ResponsibilityPermission rp WHERE rp.responsibility.responsibilityId = :responsibilityId AND rp.organization.id = :organizationId")
    void deleteAllByResponsibilityAndOrganization(
            @Param("responsibilityId") Long responsibilityId,
            @Param("organizationId") Long organizationId);
}
