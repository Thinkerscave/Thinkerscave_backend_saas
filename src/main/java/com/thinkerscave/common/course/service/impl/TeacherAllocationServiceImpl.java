package com.thinkerscave.common.course.service.impl;

import com.thinkerscave.common.course.domain.AcademicYear;
import com.thinkerscave.common.course.domain.ClassSubjectTeacher;
import com.thinkerscave.common.course.domain.Semester;
import com.thinkerscave.common.course.domain.Subject;
import com.thinkerscave.common.course.dto.TeacherAllocationDTO;
import com.thinkerscave.common.course.repository.AcademicYearRepository;
import com.thinkerscave.common.course.repository.ClassSubjectTeacherRepository;
import com.thinkerscave.common.course.repository.SemesterRepository;
import com.thinkerscave.common.course.repository.SubjectRepository;
import com.thinkerscave.common.course.service.TeacherAllocationService;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.staff.repository.StaffRepository;
import com.thinkerscave.common.student.domain.ClassEntity;
import com.thinkerscave.common.student.domain.Section;
import com.thinkerscave.common.student.repository.ClassRepository;
import com.thinkerscave.common.student.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherAllocationServiceImpl implements TeacherAllocationService {

    private final ClassSubjectTeacherRepository repository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final StaffRepository staffRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;

    @Override
    @Transactional
    public TeacherAllocationDTO allocateTeacher(TeacherAllocationDTO dto) {
        log.info("Allocating teacher {} to subject {} for class {}", dto.getTeacherId(), dto.getSubjectId(),
                dto.getClassId());

        ClassEntity classEntity = classRepository.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        AcademicYear academicYear = academicYearRepository.findById(dto.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic Year not found"));

        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        Staff teacher = staffRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff/Teacher not found"));

        Section section = null;
        if (dto.getSectionId() != null) {
            section = sectionRepository.findById(dto.getSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        }

        Semester semester = null;
        if (dto.getSemesterId() != null) {
            semester = semesterRepository.findById(dto.getSemesterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));
        }

        // Check for duplicates
        repository.findByClassEntityAndSectionAndSubjectAndTeacherAndAcademicYear(
                classEntity, section, subject, teacher, academicYear)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("This teacher is already assigned to this subject/class slot.");
                });

        ClassSubjectTeacher mapping = new ClassSubjectTeacher();
        mapping.setClassEntity(classEntity);
        mapping.setSection(section);
        mapping.setSubject(subject);
        mapping.setTeacher(teacher);
        mapping.setAcademicYear(academicYear);
        mapping.setOrganization(academicYear.getOrganization()); // Inherit Org from AcademicYear
        mapping.setSemester(semester);
        mapping.setPeriodsPerWeek(dto.getPeriodsPerWeek());
        mapping.setIsActive(true);

        ClassSubjectTeacher saved = repository.save(mapping);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public TeacherAllocationDTO updateAllocation(Long allocationId, TeacherAllocationDTO dto) {
        ClassSubjectTeacher mapping = repository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found"));

        if (dto.getPeriodsPerWeek() != null)
            mapping.setPeriodsPerWeek(dto.getPeriodsPerWeek());

        // Re-assignment logic can be complex, for now only supporting teacher switch or
        // period update
        if (dto.getTeacherId() != null && !dto.getTeacherId().equals(mapping.getTeacher().getId())) {
            Staff newTeacher = staffRepository.findById(dto.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff/Teacher not found"));
            mapping.setTeacher(newTeacher);
        }

        ClassSubjectTeacher saved = repository.save(mapping);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public void deallocateTeacher(Long allocationId) {
        ClassSubjectTeacher mapping = repository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found"));
        mapping.setIsActive(false);
        repository.save(mapping);
    }

    @Override
    public List<TeacherAllocationDTO> getAllocationsByClass(Long classId, Long academicYearId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic Year not found"));

        return repository.findByClassEntityAndAcademicYearAndIsActiveTrue(classEntity, academicYear)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<TeacherAllocationDTO> getAllocationsByTeacher(Long teacherId, Long academicYearId) {
        Staff teacher = staffRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic Year not found"));

        return repository.findByTeacherAndAcademicYearAndIsActiveTrue(teacher, academicYear)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private TeacherAllocationDTO mapToDTO(ClassSubjectTeacher entity) {
        return TeacherAllocationDTO.builder()
                .allocationId(entity.getMappingId())
                .classId(entity.getClassEntity().getClassId())
                .className(entity.getClassEntity().getClassName())
                .sectionId(entity.getSection() != null ? entity.getSection().getSectionId() : null)
                .sectionName(entity.getSection() != null ? entity.getSection().getSectionName() : null)
                .subjectId(entity.getSubject().getSubjectId())
                .subjectName(entity.getSubject().getSubjectName())
                .teacherId(entity.getTeacher().getId())
                .teacherName(entity.getTeacher().getFirstName() + " " + entity.getTeacher().getLastName())
                .academicYearId(entity.getAcademicYear().getAcademicYearId())
                .semesterId(entity.getSemester() != null ? entity.getSemester().getSemesterId() : null)
                .periodsPerWeek(entity.getPeriodsPerWeek())
                .isActive(entity.getIsActive())
                .build();
    }
}
