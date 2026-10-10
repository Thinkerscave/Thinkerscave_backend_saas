package com.thinkerscave.frontdesk.repository;

import com.thinkerscave.frontdesk.entity.FrontDeskGateOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FrontDeskGateOtpRepository extends JpaRepository<FrontDeskGateOtp, Long> {
    Optional<FrontDeskGateOtp> findFirstByOrganizationIdAndStudentIdAndVerifiedFalseAndExpiresAtAfterOrderByExpiresAtDesc(
            Long organizationId, Long studentId, LocalDateTime now);

    Optional<FrontDeskGateOtp> findFirstByOrganizationIdAndStudentIdAndVerifiedTrueOrderByExpiresAtDesc(
            Long organizationId, Long studentId);
}
