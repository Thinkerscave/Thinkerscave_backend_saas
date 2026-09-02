package com.thinkerscave.admission.repository;

import com.thinkerscave.admission.entity.AdmissionsSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdmissionsSettingRepository extends JpaRepository<AdmissionsSetting, Long> {

    Optional<AdmissionsSetting> findByOrganizationId(Long organizationId);
}
