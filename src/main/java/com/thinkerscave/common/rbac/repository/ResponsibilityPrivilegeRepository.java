package com.thinkerscave.common.rbac.repository;

import com.thinkerscave.common.rbac.domain.ResponsibilityPrivilege;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResponsibilityPrivilegeRepository extends JpaRepository<ResponsibilityPrivilege, Long> {

    List<ResponsibilityPrivilege> findByResponsibilityId(Long responsibilityId);

    void deleteByResponsibilityId(Long responsibilityId);
}
