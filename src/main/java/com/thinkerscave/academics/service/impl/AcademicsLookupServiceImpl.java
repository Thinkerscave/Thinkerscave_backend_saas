package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.response.LookupDTO;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.AcademicGroupRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.academics.service.AcademicsLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicsLookupServiceImpl implements AcademicsLookupService {

    private final AcademicYearRepository academicYearRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;

    @Override
    public List<LookupDTO> getActiveAcademicYears() {
        return academicYearRepository.findByStatusInOrderByStartDateDesc(
                        List.of(AcademicYearStatus.CURRENT, AcademicYearStatus.APPROVED, AcademicYearStatus.PREPARING))
                .stream()
                .filter(y -> Boolean.TRUE.equals(y.getActive()))
                .map(y -> new LookupDTO(
                        y.getAcademicYearId(),
                        y.getName(),
                        y.getStatus() != null ? y.getStatus().name() : null))
                .toList();
    }

    @Override
    public List<LookupDTO> getAcademicGroupsByYear(Long academicYearId) {
        return academicGroupRepository.findByAcademicYear_AcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(academicYearId)
                .stream()
                .map(A -> new LookupDTO(A.getAcademicGroupId(), A.getName()))
                .toList();
    }

    @Override
    public List<LookupDTO> getSectionsByAcademicGroup(Long academicGroupId) {
        return sectionRepository.findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(academicGroupId)
                .stream()
                .map(s -> new LookupDTO(s.getSectionId(), s.getName()))
                .toList();
    }

    @Override
    public List<LookupDTO> getActiveSubjects() {
        return subjectRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(s -> new LookupDTO(s.getSubjectId(), s.getName()))
                .toList();
    }

    @Override
    public List<LookupDTO> getSchedulesByYear(Long academicYearId) {
        return Collections.emptyList();
    }

    @Override
    public List<LookupDTO> getTemplatesBySchedule(Long scheduleId) {
        return Collections.emptyList();
    }
}
