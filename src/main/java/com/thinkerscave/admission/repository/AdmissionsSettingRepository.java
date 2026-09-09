package com.thinkerscave.admission.repository;

import com.thinkerscave.admission.entity.AdmissionsSetting;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdmissionsSettingRepository extends JpaRepository<AdmissionsSetting, Long> {

    Optional<AdmissionsSetting> findByOrganizationId(Long organizationId);

    /**
     * Locks the tenant's settings row for the duration of the current transaction so that
     * concurrent lead creations increment the round-robin cursor sequentially and safely.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AdmissionsSetting s WHERE s.organizationId = :organizationId")
    Optional<AdmissionsSetting> findByOrganizationIdForUpdate(@Param("organizationId") Long organizationId);
}
