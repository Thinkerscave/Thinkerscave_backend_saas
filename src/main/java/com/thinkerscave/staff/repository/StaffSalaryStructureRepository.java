package com.thinkerscave.staff.repository;

import com.thinkerscave.staff.entity.StaffSalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffSalaryStructureRepository extends JpaRepository<StaffSalaryStructure, Long> {

    Optional<StaffSalaryStructure> findByStaff_StaffIdAndActiveTrue(Long staffId);

    List<StaffSalaryStructure> findByStaff_StaffIdOrderByEffectiveFromDesc(Long staffId);
}
