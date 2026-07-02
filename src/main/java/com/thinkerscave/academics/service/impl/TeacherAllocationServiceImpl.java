package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.ClassTeacherAssignmentRequest;
import com.thinkerscave.academics.dto.request.SubjectAssignmentRequest;
import com.thinkerscave.academics.dto.response.ClassTeacherAssignmentResponse;
import com.thinkerscave.academics.dto.response.SubjectAssignmentResponse;
import com.thinkerscave.academics.dto.response.TeacherWorkloadResponse;
import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.ClassTeacherAssignment;
import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.entity.SubjectAssignment;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.ClassTeacherAssignmentRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.repository.SubjectAssignmentRepository;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.academics.service.TeacherAllocationService;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
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

    private final ClassTeacherAssignmentRepository ctaRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;

    @Override
    @Transactional
    public ClassTeacherAssignmentResponse assignClassTeacher(ClassTeacherAssignmentRequest request) {
        AcademicYear year = getYear(request.getAcademicYearId());
        AcademicClass cls = getClass(request.getClassId());
        AcademicSection section = request.getSectionId() != null ? getSection(request.getSectionId()) : null;

        // Deactivate any existing active assignment for same year+class+section
        ctaRepository.findByAcademicYear_AcademicYearIdAndAcademicClass_ClassIdAndAcademicSection_SectionIdAndActiveTrue(
                request.getAcademicYearId(), request.getClassId(), request.getSectionId())
                .ifPresent(existing -> {
                    existing.setActive(false);
                    ctaRepository.save(existing);
                });

        ClassTeacherAssignment cta = new ClassTeacherAssignment();
        cta.setAcademicYear(year);
        cta.setAcademicClass(cls);
        cta.setAcademicSection(section);
        cta.setTeacherId(request.getTeacherId());
        cta.setEffectiveFrom(request.getEffectiveFrom());
        cta.setActive(true);
        cta.setRemarks(request.getRemarks());
        return toCtaResponse(ctaRepository.save(cta));
    }

    @Override
    @Transactional(readOnly = true)
    public ClassTeacherAssignmentResponse getClassTeacherAssignment(Long assignmentId) {
        ClassTeacherAssignment cta = ctaRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
        return toCtaResponse(cta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassTeacherAssignmentResponse> getClassTeacherAssignments(Long yearId, Long classId, Long sectionId) {
        return ctaRepository.findByAcademicYear_AcademicYearIdAndAcademicClass_ClassIdOrderByCreatedOnDesc(yearId, classId)
                .stream().map(this::toCtaResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeClassTeacher(Long assignmentId) {
        ClassTeacherAssignment cta = ctaRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
        cta.setActive(false);
        ctaRepository.save(cta);
    }

    @Override
    @Transactional
    public SubjectAssignmentResponse assignSubject(SubjectAssignmentRequest request) {
        if (subjectAssignmentRepository.existsByAcademicYear_AcademicYearIdAndAcademicClass_ClassIdAndAcademicSection_SectionIdAndSubject_SubjectIdAndActiveTrue(
                request.getAcademicYearId(), request.getClassId(), request.getSectionId(), request.getSubjectId())) {
            throw new AlreadyExistsException("Subject is already assigned to this class/section for the selected academic year");
        }
        AcademicYear year = getYear(request.getAcademicYearId());
        AcademicClass cls = getClass(request.getClassId());
        AcademicSection section = request.getSectionId() != null ? getSection(request.getSectionId()) : null;
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.getSubjectId()));

        SubjectAssignment sa = new SubjectAssignment();
        sa.setAcademicYear(year);
        sa.setAcademicClass(cls);
        sa.setAcademicSection(section);
        sa.setSubject(subject);
        sa.setTeacherId(request.getTeacherId());
        sa.setPeriodsPerWeek(request.getPeriodsPerWeek());
        sa.setActive(true);
        sa.setRemarks(request.getRemarks());
        return toSaResponse(subjectAssignmentRepository.save(sa));
    }

    @Override
    @Transactional
    public SubjectAssignmentResponse updateSubjectAssignment(Long assignmentId, SubjectAssignmentRequest request) {
        SubjectAssignment sa = subjectAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject assignment not found: " + assignmentId));
        sa.setTeacherId(request.getTeacherId());
        sa.setPeriodsPerWeek(request.getPeriodsPerWeek());
        sa.setRemarks(request.getRemarks());
        return toSaResponse(subjectAssignmentRepository.save(sa));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectAssignmentResponse> getSubjectAssignments(Long yearId, Long classId, Long sectionId) {
        return subjectAssignmentRepository.findByAcademicYear_AcademicYearIdAndAcademicClass_ClassIdOrderBySubject_SubjectNameAsc(yearId, classId)
                .stream().map(this::toSaResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeSubjectAssignment(Long assignmentId) {
        SubjectAssignment sa = subjectAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject assignment not found: " + assignmentId));
        sa.setActive(false);
        subjectAssignmentRepository.save(sa);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherWorkloadResponse getTeacherWorkload(Long teacherId, Long academicYearId) {
        List<SubjectAssignment> assignments = subjectAssignmentRepository
                .findByTeacherIdAndAcademicYear_AcademicYearIdAndActiveTrue(teacherId, academicYearId);
        int total = assignments.stream()
                .mapToInt(sa -> sa.getPeriodsPerWeek() != null ? sa.getPeriodsPerWeek() : 0)
                .sum();
        List<TeacherWorkloadResponse.SubjectAllocationItem> items = assignments.stream()
                .map(sa -> TeacherWorkloadResponse.SubjectAllocationItem.builder()
                        .assignmentId(sa.getSubjectAssignmentId())
                        .subjectName(sa.getSubject().getSubjectName())
                        .className(sa.getAcademicClass().getClassName())
                        .sectionName(sa.getAcademicSection() != null ? sa.getAcademicSection().getSectionName() : null)
                        .periodsPerWeek(sa.getPeriodsPerWeek())
                        .build())
                .collect(Collectors.toList());
        return TeacherWorkloadResponse.builder()
                .teacherId(teacherId)
                .academicYearId(academicYearId)
                .totalPeriodsPerWeek(total)
                .allocations(items)
                .build();
    }

    // ---- helpers ----

    private AcademicYear getYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + id));
    }

    private AcademicClass getClass(Long id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic class not found: " + id));
    }

    private AcademicSection getSection(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + id));
    }

    private ClassTeacherAssignmentResponse toCtaResponse(ClassTeacherAssignment cta) {
        return ClassTeacherAssignmentResponse.builder()
                .assignmentId(cta.getAssignmentId())
                .academicYearId(cta.getAcademicYear().getAcademicYearId())
                .yearCode(cta.getAcademicYear().getYearCode())
                .classId(cta.getAcademicClass().getClassId())
                .className(cta.getAcademicClass().getClassName())
                .sectionId(cta.getAcademicSection() != null ? cta.getAcademicSection().getSectionId() : null)
                .sectionName(cta.getAcademicSection() != null ? cta.getAcademicSection().getSectionName() : null)
                .teacherId(cta.getTeacherId())
                .effectiveFrom(cta.getEffectiveFrom())
                .effectiveTo(cta.getEffectiveTo())
                .active(cta.getActive())
                .createdOn(cta.getCreatedOn())
                .build();
    }

    private SubjectAssignmentResponse toSaResponse(SubjectAssignment sa) {
        return SubjectAssignmentResponse.builder()
                .subjectAssignmentId(sa.getSubjectAssignmentId())
                .academicYearId(sa.getAcademicYear().getAcademicYearId())
                .classId(sa.getAcademicClass().getClassId())
                .className(sa.getAcademicClass().getClassName())
                .sectionId(sa.getAcademicSection() != null ? sa.getAcademicSection().getSectionId() : null)
                .sectionName(sa.getAcademicSection() != null ? sa.getAcademicSection().getSectionName() : null)
                .subjectId(sa.getSubject().getSubjectId())
                .subjectName(sa.getSubject().getSubjectName())
                .teacherId(sa.getTeacherId())
                .periodsPerWeek(sa.getPeriodsPerWeek())
                .active(sa.getActive())
                .remarks(sa.getRemarks())
                .build();
    }
}
