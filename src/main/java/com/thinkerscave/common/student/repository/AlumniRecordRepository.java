package com.thinkerscave.common.student.repository;

import com.thinkerscave.common.student.domain.AlumniRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlumniRecordRepository extends JpaRepository<AlumniRecord, Long> {

    List<AlumniRecord> findByOrganizationIdOrderByGraduationDateDesc(Long organizationId);

    long countByOrganizationId(Long organizationId);
}
