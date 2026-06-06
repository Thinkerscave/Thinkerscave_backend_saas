package com.thinkerscave.common.staff.repository;

import com.thinkerscave.common.staff.domain.TeachingProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeachingProfileRepository extends JpaRepository<TeachingProfile, Long> {

    Optional<TeachingProfile> findByStaffIdAndOrganizationId(Long staffId, Long organizationId);
}
