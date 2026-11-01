package com.thinkerscave.frontdesk.repository;

import com.thinkerscave.frontdesk.entity.FrontDeskSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FrontDeskSettingRepository extends JpaRepository<FrontDeskSetting, Long> {
    Optional<FrontDeskSetting> findByOrganizationId(Long organizationId);
}
