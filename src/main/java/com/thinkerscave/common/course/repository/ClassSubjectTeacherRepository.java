package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.AcademicYear;
import com.thinkerscave.common.course.domain.ClassSubjectTeacher;
import com.thinkerscave.common.course.domain.Subject;
import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.student.domain.ClassEntity;
import com.thinkerscave.common.student.domain.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSubjectTeacherRepository extends JpaRepository<ClassSubjectTeacher, Long> {

    List<ClassSubjectTeacher> findByClassEntityAndAcademicYearAndIsActiveTrue(ClassEntity classEntity,
            AcademicYear academicYear);

    List<ClassSubjectTeacher> findByTeacherAndAcademicYearAndIsActiveTrue(Staff teacher, AcademicYear academicYear);

    List<ClassSubjectTeacher> findByClassEntityAndSectionAndAcademicYearAndIsActiveTrue(ClassEntity classEntity,
            Section section, AcademicYear academicYear);

    Optional<ClassSubjectTeacher> findByClassEntityAndSectionAndSubjectAndAcademicYearAndIsActiveTrue(
            ClassEntity classEntity, Section section, Subject subject, AcademicYear academicYear);

    // Check if a teacher is already assigned to this specific slot
    Optional<ClassSubjectTeacher> findByClassEntityAndSectionAndSubjectAndTeacherAndAcademicYear(
            ClassEntity classEntity, Section section, Subject subject, Staff teacher, AcademicYear academicYear);
}
