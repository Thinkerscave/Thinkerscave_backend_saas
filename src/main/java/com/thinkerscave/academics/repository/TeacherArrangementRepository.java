package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TeacherArrangement;
import com.thinkerscave.academics.enums.ArrangementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TeacherArrangementRepository extends JpaRepository<TeacherArrangement, Long> {

    List<TeacherArrangement> findByStatusOrderByArrangementDateDesc(ArrangementStatus status);

    List<TeacherArrangement> findByArrangementDateAndStatusOrderByArrangementDateDesc(
            LocalDate date, ArrangementStatus status);

    List<TeacherArrangement> findByAbsentTeacherIdOrSubstituteTeacherIdOrderByArrangementDateDesc(
            Long absentId, Long subId);
}
