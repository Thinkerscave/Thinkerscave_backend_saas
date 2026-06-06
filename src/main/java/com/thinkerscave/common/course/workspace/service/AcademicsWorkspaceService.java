package com.thinkerscave.common.course.workspace.service;

import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.course.domain.AcademicYear;
import com.thinkerscave.common.course.domain.ClassSubjectTeacher;
import com.thinkerscave.common.course.repository.AcademicYearRepository;
import com.thinkerscave.common.course.repository.ClassSubjectTeacherRepository;
import com.thinkerscave.common.course.repository.SubjectRepository;
import com.thinkerscave.common.course.workspace.dto.AcademicsWorkspaceDtos.*;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.repository.OrganizationRepository;
import com.thinkerscave.common.student.domain.ClassEntity;
import com.thinkerscave.common.student.domain.Section;
import com.thinkerscave.common.student.repository.ClassRepository;
import com.thinkerscave.common.student.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicsWorkspaceService {

    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectTeacherRepository cstRepository;
    private final AcademicYearRepository academicYearRepository;
    private final OrganizationRepository organizationRepository;

    public AcademicsKpi kpi() {
        Long orgId = orgId();
        Optional<Organisation> org = organizationRepository.findById(orgId);
        long classes = classRepository.countByOrganizationId(orgId);
        long sections = sectionRepository.countByOrganizationId(orgId);
        long subjects = org.map(o -> (long) subjectRepository.findByOrganizationAndIsActiveTrue(o).size()).orElse(0L);

        AcademicYear current = org.flatMap(o -> academicYearRepository.findByOrganizationAndIsCurrentTrue(o)).orElse(null);

        long teachersAssigned = 0L;
        if (current != null) {
            Set<Long> distinct = new HashSet<>();
            for (ClassEntity ce : classRepository.findByOrganizationId(orgId)) {
                cstRepository.findByClassEntityAndAcademicYearAndIsActiveTrue(ce, current).stream()
                        .map(ClassSubjectTeacher::getTeacher)
                        .filter(t -> t != null && t.getId() != null)
                        .forEach(t -> distinct.add(t.getId()));
            }
            teachersAssigned = distinct.size();
        }

        return AcademicsKpi.builder()
                .classes(classes)
                .sections(sections)
                .subjects(subjects)
                .teachersAssigned(teachersAssigned)
                .activeYearCode(current != null ? current.getYearCode() : null)
                .build();
    }

    public AcademicsStructure structure() {
        Long orgId = orgId();
        Optional<Organisation> org = organizationRepository.findById(orgId);
        AcademicYear current = org.flatMap(o -> academicYearRepository.findByOrganizationAndIsCurrentTrue(o)).orElse(null);

        List<ClassEntity> classes = classRepository.findByOrganizationId(orgId);
        Map<Long, Long> sectionsByClass = sectionRepository.findByOrganizationId(orgId).stream()
                .filter(s -> s.getClassEntity() != null)
                .collect(Collectors.groupingBy(s -> s.getClassEntity().getClassId(), Collectors.counting()));

        List<ClassStructureCard> cards = classes.stream().map(ce -> {
            int sectionCount = sectionsByClass.getOrDefault(ce.getClassId(), 0L).intValue();
            int subjectCount = 0;
            int teacherCount = 0;
            if (current != null) {
                List<ClassSubjectTeacher> rows = cstRepository.findByClassEntityAndAcademicYearAndIsActiveTrue(ce, current);
                subjectCount = (int) rows.stream()
                        .map(r -> r.getSubject() != null ? r.getSubject().getSubjectId() : null)
                        .filter(java.util.Objects::nonNull)
                        .distinct().count();
                teacherCount = (int) rows.stream()
                        .map(r -> r.getTeacher() != null ? r.getTeacher().getId() : null)
                        .filter(java.util.Objects::nonNull)
                        .distinct().count();
            }
            return ClassStructureCard.builder()
                    .classId(ce.getClassId())
                    .className(ce.getClassName())
                    .sectionCount(sectionCount)
                    .subjectCount(subjectCount)
                    .teacherCount(teacherCount)
                    .build();
        }).collect(Collectors.toList());

        return AcademicsStructure.builder().classes(cards).build();
    }

    private Long orgId() {
        Long id = OrganizationContext.getOrganizationId();
        return id != null ? id : 1L;
    }
}
