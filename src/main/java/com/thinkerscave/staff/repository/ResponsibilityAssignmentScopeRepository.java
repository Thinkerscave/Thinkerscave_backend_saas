package com.thinkerscave.staff.repository;

import com.thinkerscave.staff.entity.ResponsibilityAssignmentScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ResponsibilityAssignmentScopeRepository extends JpaRepository<ResponsibilityAssignmentScope, Long> {

    List<ResponsibilityAssignmentScope> findByAssignmentIdIn(Collection<Long> assignmentIds);
}
