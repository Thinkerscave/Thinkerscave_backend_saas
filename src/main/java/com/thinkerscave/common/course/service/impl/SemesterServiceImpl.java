package com.thinkerscave.common.course.service.impl;

import com.thinkerscave.common.course.domain.AcademicYear;
import com.thinkerscave.common.course.domain.Semester;
import com.thinkerscave.common.course.dto.SemesterDTO;
import com.thinkerscave.common.course.repository.AcademicYearRepository;
import com.thinkerscave.common.course.repository.SemesterRepository;
import com.thinkerscave.common.course.service.SemesterService;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemesterServiceImpl implements SemesterService {

        private final SemesterRepository semesterRepository;
        private final AcademicYearRepository academicYearRepository;

        @Override
        @Transactional
        public SemesterDTO createSemester(SemesterDTO dto) {
                AcademicYear year = academicYearRepository.findById(dto.getAcademicYearId())
                                .orElseThrow(() -> new ResourceNotFoundException("Academic Year not found"));

                Semester semester = new Semester();
                semester.setSemesterName(dto.getSemesterName());
                semester.setSemesterNumber(dto.getSemesterNumber());
                semester.setAcademicYear(year);
                semester.setOrganization(year.getOrganization()); // Inherit Org from Year
                semester.setStartDate(dto.getStartDate());
                semester.setEndDate(dto.getEndDate());
                semester.setDescription(dto.getDescription());
                semester.setIsActive(true);
                semester.setIsCurrent(false);

                Semester saved = semesterRepository.save(semester);
                return mapToDTO(saved);
        }

        @Override
        @Transactional
        public SemesterDTO updateSemester(Long semesterId, SemesterDTO dto) {
                Semester semester = semesterRepository.findById(semesterId)
                                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));

                semester.setSemesterName(dto.getSemesterName());
                semester.setSemesterNumber(dto.getSemesterNumber());
                semester.setStartDate(dto.getStartDate());
                semester.setEndDate(dto.getEndDate());
                semester.setDescription(dto.getDescription());

                Semester saved = semesterRepository.save(semester);
                return mapToDTO(saved);
        }

        @Override
        public SemesterDTO getSemester(Long semesterId) {
                Semester semester = semesterRepository.findById(semesterId)
                                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));
                return mapToDTO(semester);
        }

        @Override
        public List<SemesterDTO> getSemestersByAcademicYear(Long academicYearId) {
                AcademicYear year = academicYearRepository.findById(academicYearId)
                                .orElseThrow(() -> new ResourceNotFoundException("Academic Year not found"));
                return semesterRepository.findByAcademicYear(year).stream()
                                .map(this::mapToDTO)
                                .collect(Collectors.toList());
        }

        @Override
        public List<SemesterDTO> getActiveSemesters(Long academicYearId) {
                AcademicYear year = academicYearRepository.findById(academicYearId)
                                .orElseThrow(() -> new ResourceNotFoundException("Academic Year not found"));
                return semesterRepository.findByAcademicYearAndIsActiveTrue(year).stream()
                                .map(this::mapToDTO)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public void setCurrentSemester(Long semesterId) {
                Semester newCurrent = semesterRepository.findById(semesterId)
                                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));

                // Disable current flag for all other semesters in this year
                List<Semester> allSemesters = semesterRepository.findByAcademicYear(newCurrent.getAcademicYear());
                for (Semester s : allSemesters) {
                        s.setIsCurrent(s.getSemesterId().equals(semesterId));
                }
                semesterRepository.saveAll(allSemesters);
        }

        @Override
        public SemesterDTO getCurrentSemester(Long academicYearId) {
                AcademicYear year = academicYearRepository.findById(academicYearId)
                                .orElseThrow(() -> new ResourceNotFoundException("Academic Year not found"));

                // Assuming logic to find current, or first active current, or throw
                return semesterRepository.findByAcademicYear(year).stream()
                                .filter(Semester::getIsCurrent)
                                .findFirst()
                                .map(this::mapToDTO)
                                .orElse(null);
        }

        @Override
        @Transactional
        public void deleteSemester(Long semesterId) {
                Semester semester = semesterRepository.findById(semesterId)
                                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));
                semester.setIsActive(false);
                semesterRepository.save(semester);
        }

        private SemesterDTO mapToDTO(Semester entity) {
                return SemesterDTO.builder()
                                .semesterId(entity.getSemesterId())
                                .semesterName(entity.getSemesterName())
                                .semesterNumber(entity.getSemesterNumber())
                                .academicYearId(entity.getAcademicYear().getAcademicYearId())
                                .academicYearName(entity.getAcademicYear().getYearName())
                                .startDate(entity.getStartDate())
                                .endDate(entity.getEndDate())
                                .isCurrent(entity.getIsCurrent())
                                .isActive(entity.getIsActive())
                                .description(entity.getDescription())
                                .build();
        }
}
