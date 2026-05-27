package com.thinkerscave.common.rbac.repository;

import com.thinkerscave.common.rbac.domain.UserResponsibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserResponsibilityRepository extends JpaRepository<UserResponsibility, Long> {

    List<UserResponsibility> findByUserIdAndActive(Long userId, boolean active);

    List<UserResponsibility> findByResponsibilityIdAndActive(Long responsibilityId, boolean active);

    List<UserResponsibility> findByUserIdAndOrganizationIdAndActive(
            Long userId, Long organizationId, boolean active);
}
