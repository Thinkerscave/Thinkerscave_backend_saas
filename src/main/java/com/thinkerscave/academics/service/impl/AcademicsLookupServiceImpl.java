package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.response.LookupDTO;
import com.thinkerscave.academics.repository.AcademicScheduleRepository;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.academics.repository.TimetableTemplateRepository;
import com.thinkerscave.academics.service.AcademicsLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcademicsLookupServiceImpl implements AcademicsLookupService {

    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicScheduleRepository scheduleRepository;
    private final TimetableTemplateRepository templateRepository;

    @Override
    public List<LookupDTO> getActiveAcademicYears() {
        return academicYearRepository.findByActiveOrderByStartDateDesc(true)
                .stream()
                .map(y -> new LookupDTO(y.getAcademicYearId(), y.getYearCode() + " - " + y.getYearName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<LookupDTO> getClassesByYear(Long academicYearId) {
        return classRepository.findByAcademicYear_AcademicYearIdAndActiveOrderByDisplayOrderAsc(academicYearId, true)
                .stream()
                .map(c -> new LookupDTO(c.getClassId(), c.getClassName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<LookupDTO> getSectionsByClass(Long classId) {
        return sectionRepository.findByAcademicClass_ClassIdAndActiveOrderBySectionNameAsc(classId, true)
                .stream()
                .map(s -> new LookupDTO(s.getSectionId(), s.getSectionName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<LookupDTO> getActiveSubjects() {
        return subjectRepository.findByActiveOrderBySubjectNameAsc(true)
                .stream()
                .map(s -> new LookupDTO(s.getSubjectId(), s.getSubjectName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<LookupDTO> getSchedulesByYear(Long academicYearId) {
        return scheduleRepository.findByAcademicYear_AcademicYearIdAndActiveOrderByStartDateAsc(academicYearId, true)
                .stream()
                .map(s -> new LookupDTO(s.getScheduleId(), s.getScheduleName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<LookupDTO> getTemplatesBySchedule(Long scheduleId) {
        return templateRepository.findByAcademicSchedule_ScheduleIdAndActiveOrderByTemplateNameAsc(scheduleId, true)
                .stream()
                .map(t -> new LookupDTO(t.getTemplateId(), t.getTemplateName()))
                .collect(Collectors.toList());
    }
}
