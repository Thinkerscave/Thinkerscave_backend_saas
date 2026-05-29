package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.AcademicSetting;
import com.thinkerscave.common.orgm.domain.Organisation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicSettingRepository extends JpaRepository<AcademicSetting, Long> {

    @EntityGraph(attributePaths = { "organization" })
    List<AcademicSetting> findByOrganizationAndIsActiveTrueOrderByCategoryAscSettingKeyAsc(Organisation organization);

    @EntityGraph(attributePaths = { "organization" })
    Optional<AcademicSetting> findByOrganizationAndSettingKey(Organisation organization, String settingKey);

    @EntityGraph(attributePaths = { "organization" })
    Optional<AcademicSetting> findBySettingIdAndOrganization(Long settingId, Organisation organization);
}