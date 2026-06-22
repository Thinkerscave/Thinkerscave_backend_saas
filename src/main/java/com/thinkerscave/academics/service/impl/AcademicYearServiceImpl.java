package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.AcademicYearRequest;
import com.thinkerscave.academics.dto.request.CloneAcademicYearRequest;
import com.thinkerscave.academics.dto.response.AcademicYearResponse;
import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicSchedule;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.ClassTeacherAssignment;
import com.thinkerscave.academics.entity.SubjectAssignment;
import com.thinkerscave.academics.entity.TimetableTemplate;
import com.thinkerscave.academics.repository.AcademicScheduleRepository;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.ClassTeacherAssignmentRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.repository.SubjectAssignmentRepository;
import com.thinkerscave.academics.repository.TimetableTemplateRepository;
import com.thinkerscave.academics.service.AcademicYearService;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Tag(name = "Academic Year Service")
public class AcademicYearServiceImpl implements AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final ClassTeacherAssignmentRepository classTeacherAssignmentRepository;
    private final AcademicScheduleRepository academicScheduleRepository;
    private final TimetableTemplateRepository timetableTemplateRepository;

    @Override
    @Transactional
    public AcademicYearResponse create(AcademicYearRequest request) {
        if (academicYearRepository.existsByYearCode(request.getYearCode())) {
            throw new AlreadyExistsException("Academic year with code '" + request.getYearCode() + "' already exists");
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }
        AcademicYear year = new AcademicYear();
        mapRequestToEntity(request, year);
        year.setCurrentYear(false);
        year.setActive(true);
        return toResponse(academicYearRepository.save(year));
    }

    @Override
    @Transactional
    public AcademicYearResponse update(Long id, AcademicYearRequest request) {
        AcademicYear year = findById(id);
        if (academicYearRepository.existsByYearCodeAndAcademicYearIdNot(request.getYearCode(), id)) {
            throw new AlreadyExistsException("Academic year code '" + request.getYearCode() + "' is already in use");
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }
        mapRequestToEntity(request, year);
        return toResponse(academicYearRepository.save(year));
    }

    @Override
    public AcademicYearResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    public List<AcademicYearResponse> getAll() {
        return academicYearRepository.findByActiveOrderByStartDateDesc(true)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AcademicYearResponse setCurrentYear(Long id) {
        AcademicYear year = findById(id);
        if (!Boolean.TRUE.equals(year.getActive())) {
            throw new BadRequestException("Cannot set an inactive year as current");
        }
        academicYearRepository.clearCurrentYear();
        year.setCurrentYear(true);
        return toResponse(academicYearRepository.save(year));
    }

    @Override
    @Transactional
    public AcademicYearResponse deactivate(Long id) {
        AcademicYear year = findById(id);
        if (Boolean.TRUE.equals(year.getCurrentYear())) {
            throw new BadRequestException("Cannot deactivate the currently active academic year");
        }
        year.setActive(false);
        return toResponse(academicYearRepository.save(year));
    }

    @Override
    @Transactional
    public AcademicYearResponse clone(Long id, CloneAcademicYearRequest request) {
        if (academicYearRepository.existsByYearCode(request.getNewYearCode())) {
            throw new AlreadyExistsException("Academic year with code '" + request.getNewYearCode() + "' already exists");
        }
        AcademicYear source = findById(id);

        AcademicYear newYear = new AcademicYear();
        newYear.setYearCode(request.getNewYearCode());
        newYear.setYearName(request.getNewYearName());
        newYear.setStartDate(source.getStartDate());
        newYear.setEndDate(source.getEndDate());
        newYear.setCurrentYear(false);
        newYear.setActive(true);
        newYear = academicYearRepository.save(newYear);

        if (request.isCopyClasses()) {
            List<AcademicClass> sourceClasses = classRepository.findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(id);
            for (AcademicClass sc : sourceClasses) {
                AcademicClass nc = new AcademicClass();
                nc.setAcademicYear(newYear);
                nc.setClassCode(sc.getClassCode());
                nc.setClassName(sc.getClassName());
                nc.setAcademicStage(sc.getAcademicStage());
                nc.setDisplayOrder(sc.getDisplayOrder());
                nc.setActive(true);
                nc = classRepository.save(nc);

                if (request.isCopySections()) {
                    List<AcademicSection> sections = sectionRepository.findByAcademicClass_ClassIdOrderBySectionNameAsc(sc.getClassId());
                    for (AcademicSection ss : sections) {
                        AcademicSection ns = new AcademicSection();
                        ns.setAcademicClass(nc);
                        ns.setSectionName(ss.getSectionName());
                        ns.setCapacity(ss.getCapacity());
                        ns.setActive(true);
                        sectionRepository.save(ns);
                    }
                }

                if (request.isCopyTeacherAllocations()) {
                    List<ClassTeacherAssignment> ctas = classTeacherAssignmentRepository
                            .findByAcademicYear_AcademicYearIdAndAcademicClass_ClassIdOrderByCreatedOnDesc(id, sc.getClassId());
                    for (ClassTeacherAssignment cta : ctas) {
                        ClassTeacherAssignment ncta = new ClassTeacherAssignment();
                        ncta.setAcademicYear(newYear);
                        ncta.setAcademicClass(nc);
                        ncta.setTeacherId(cta.getTeacherId());
                        ncta.setEffectiveFrom(cta.getEffectiveFrom());
                        ncta.setActive(true);
                        classTeacherAssignmentRepository.save(ncta);
                    }
                }
            }
        }

        if (request.isCopySubjects() && request.isCopyClasses()) {
            List<AcademicClass> sourceClasses = classRepository.findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(id);
            List<AcademicClass> newClasses = classRepository.findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(newYear.getAcademicYearId());
            for (int i = 0; i < sourceClasses.size() && i < newClasses.size(); i++) {
                AcademicClass sc = sourceClasses.get(i);
                AcademicClass nc = newClasses.get(i);
                List<SubjectAssignment> assignments = subjectAssignmentRepository
                        .findByAcademicYear_AcademicYearIdAndAcademicClass_ClassIdOrderBySubject_SubjectNameAsc(id, sc.getClassId());
                for (SubjectAssignment sa : assignments) {
                    SubjectAssignment nsa = new SubjectAssignment();
                    nsa.setAcademicYear(newYear);
                    nsa.setAcademicClass(nc);
                    nsa.setSubject(sa.getSubject());
                    nsa.setTeacherId(sa.getTeacherId());
                    nsa.setPeriodsPerWeek(sa.getPeriodsPerWeek());
                    nsa.setActive(true);
                    subjectAssignmentRepository.save(nsa);
                }
            }
        }

        if (request.isCopySchedules()) {
            List<AcademicSchedule> schedules = academicScheduleRepository
                    .findByAcademicYear_AcademicYearIdOrderByStartDateAsc(id);
            for (AcademicSchedule ss : schedules) {
                AcademicSchedule ns = new AcademicSchedule();
                ns.setAcademicYear(newYear);
                ns.setScheduleName(ss.getScheduleName());
                ns.setStartDate(ss.getStartDate());
                ns.setEndDate(ss.getEndDate());
                ns.setActive(true);
                ns = academicScheduleRepository.save(ns);

                if (request.isCopyTemplates()) {
                    List<TimetableTemplate> templates = timetableTemplateRepository
                            .findByAcademicSchedule_ScheduleIdOrderByTemplateNameAsc(ss.getScheduleId());
                    for (TimetableTemplate st : templates) {
                        TimetableTemplate nt = new TimetableTemplate();
                        nt.setAcademicSchedule(ns);
                        nt.setTemplateName(st.getTemplateName());
                        nt.setActive(true);
                        timetableTemplateRepository.save(nt);
                    }
                }
            }
        }

        log.info("Cloned academic year {} to new year {}", id, newYear.getAcademicYearId());
        return toResponse(newYear);
    }

    // ---- helpers ----

    private AcademicYear findById(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + id));
    }

    private void mapRequestToEntity(AcademicYearRequest request, AcademicYear year) {
        year.setYearCode(request.getYearCode());
        year.setYearName(request.getYearName());
        year.setStartDate(request.getStartDate());
        year.setEndDate(request.getEndDate());
        year.setRemarks(request.getRemarks());
    }

    private AcademicYearResponse toResponse(AcademicYear year) {
        return AcademicYearResponse.builder()
                .academicYearId(year.getAcademicYearId())
                .yearCode(year.getYearCode())
                .yearName(year.getYearName())
                .startDate(year.getStartDate())
                .endDate(year.getEndDate())
                .currentYear(year.getCurrentYear())
                .active(year.getActive())
                .remarks(year.getRemarks())
                .createdBy(year.getCreatedBy())
                .createdOn(year.getCreatedOn())
                .build();
    }
}
