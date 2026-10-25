package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.response.TeacherArrangementEligibleSlotResponse;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.TeacherAllocationTeacherRole;
import com.thinkerscave.academics.enums.TimetableEntryType;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.*;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherArrangementServiceImplTest {

    private static final Long YEAR_ID = 7L;
    private static final Long VERSION_ID = 21L;
    private static final LocalDate ABSENCE_DATE = LocalDate.of(2027, 4, 26);

    @Mock private AcademicYearRepository yearRepository;
    @Mock private TimetableVersionRepository versionRepository;
    @Mock private TimetableEntryRepository entryRepository;
    @Mock private TeacherAllocationTeacherRepository allocationTeacherRepository;
    @Mock private TeacherArrangementRepository arrangementRepository;
    @Mock private TeacherArrangementSlotRepository arrangementSlotRepository;
    @Mock private AcademicsAccessGuard accessGuard;

    private TeacherArrangementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TeacherArrangementServiceImpl(
                yearRepository, versionRepository, entryRepository, allocationTeacherRepository,
                arrangementRepository, arrangementSlotRepository, accessGuard);
    }

    @Test
    void eligibleCandidatesAreAssignedTeachersWhoAreFreeInThePeriod() {
        arrangeSchedule(Set.of());

        List<TeacherArrangementEligibleSlotResponse> slots =
                service.eligibleSlots(YEAR_ID, ABSENCE_DATE, 1L);

        assertEquals(1, slots.size());
        assertEquals(List.of(new TeacherArrangementEligibleSlotResponse.StaffOption(2L, "Available Teacher")),
                slots.get(0).candidates());
        assertEquals("Class II", slots.get(0).academicGroupName());
        assertEquals("A", slots.get(0).sectionName());
    }

    @Test
    void pendingSubstituteReservationRemovesCandidateForTheSamePeriod() {
        arrangeSchedule(Set.of(2L));

        List<TeacherArrangementEligibleSlotResponse> slots =
                service.eligibleSlots(YEAR_ID, ABSENCE_DATE, 1L);

        assertEquals(1, slots.size());
        assertEquals(List.of(), slots.get(0).candidates());
    }

    private void arrangeSchedule(Set<Long> reservedSubstitutes) {
        AcademicYear year = new AcademicYear();
        year.setAcademicYearId(YEAR_ID);
        year.setName("2027-28");
        year.setStartDate(LocalDate.of(2027, 4, 22));
        year.setEndDate(LocalDate.of(2028, 3, 22));
        year.setActive(true);

        TimetableVersion version = new TimetableVersion();
        version.setTimetableVersionId(VERSION_ID);
        version.setStatus(TimetableStatus.PUBLISHED);

        Staff absent = staff(1L, "Absent", "Teacher");
        Staff available = staff(2L, "Available", "Teacher");
        Staff busy = staff(3L, "Busy", "Teacher");
        TeacherAllocation absentAllocation = allocation(11L);
        TeacherAllocation busyAllocation = allocation(13L);

        when(yearRepository.findById(YEAR_ID)).thenReturn(Optional.of(year));
        when(versionRepository.findByAcademicYear_AcademicYearIdAndStatus(YEAR_ID, TimetableStatus.PUBLISHED))
                .thenReturn(Optional.of(version));
        when(allocationTeacherRepository.findActiveByYear(YEAR_ID)).thenReturn(List.of(
                primary(absentAllocation, absent),
                primary(allocation(12L), available),
                primary(busyAllocation, busy)));
        when(entryRepository.findGridEntries(VERSION_ID)).thenReturn(List.of(
                entry(101L, absentAllocation),
                entry(103L, busyAllocation)));
        when(arrangementSlotRepository.findReservedSubstituteIds(
                eq(ABSENCE_DATE), eq(31L), anyCollection(), isNull()))
                .thenReturn(reservedSubstitutes);
    }

    private Staff staff(Long id, String firstName, String lastName) {
        Staff staff = new Staff();
        staff.setStaffId(id);
        staff.setFirstName(firstName);
        staff.setLastName(lastName);
        staff.setActive(true);
        staff.setEmploymentStatus(EmploymentStatus.ACTIVE);
        return staff;
    }

    private TeacherAllocation allocation(Long id) {
        TeacherAllocation allocation = new TeacherAllocation();
        allocation.setTeacherAllocationId(id);
        allocation.setActive(true);
        return allocation;
    }

    private TeacherAllocationTeacher primary(TeacherAllocation allocation, Staff staff) {
        TeacherAllocationTeacher assignment = new TeacherAllocationTeacher();
        assignment.setTeacherAllocation(allocation);
        assignment.setStaff(staff);
        assignment.setRole(TeacherAllocationTeacherRole.PRIMARY);
        assignment.setEffectiveFrom(LocalDate.of(2027, 4, 22));
        assignment.setActive(true);
        return assignment;
    }

    private TimetableEntry entry(Long id, TeacherAllocation allocation) {
        TimetablePeriod period = new TimetablePeriod();
        period.setTimetablePeriodId(31L);
        period.setPeriodNumber((short) 1);
        period.setStartTime(LocalTime.of(8, 30));
        period.setEndTime(LocalTime.of(9, 15));

        AcademicClass academicClass = new AcademicClass();
        academicClass.setName("Class II");
        AcademicSection section = new AcademicSection();
        section.setName("A");
        section.setAcademicClass(academicClass);

        TimetableEntry entry = new TimetableEntry();
        entry.setTimetableEntryId(id);
        entry.setDayOfWeek(DayOfWeek.MONDAY);
        entry.setTimetablePeriod(period);
        entry.setTeacherAllocation(allocation);
        entry.setSection(section);
        entry.setEntryType(TimetableEntryType.SUBJECT);
        entry.setSubjectNameSnapshot("English");
        return entry;
    }
}