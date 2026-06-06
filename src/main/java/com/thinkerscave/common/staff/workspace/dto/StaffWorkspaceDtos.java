package com.thinkerscave.common.staff.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Single-file DTO container for the Staff Management workspace. Mirrors the
 * inspiration spec: Directory, Profile 360, Responsibilities, Leave & Availability,
 * Documents Vault, Alumni Staff.
 */
public final class StaffWorkspaceDtos {

    private StaffWorkspaceDtos() {
    }

    // ---------------- KPI ----------------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffKpi {
        private long totalEmployees;
        private long teachingStaff;
        private long nonTeachingStaff;
        private long onLeaveToday;
        private long newJoiners;
    }

    // ---------------- Directory ----------------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffSearchRequest {
        private String search;
        private Long departmentId;
        private Long branchId;
        private String employmentType; // TEACHING / NON_TEACHING / ALL
        private Boolean activeOnly;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffDirectoryCard {
        private Long staffId;
        private String staffCode;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private String mobileNumber;
        private String gender;
        private String departmentName;
        private String branchName;
        private String designation;
        private LocalDate hireDate;
        private String photoUrl;
        private Boolean isActive;
        private String availabilityStatus; // PRESENT / ON_LEAVE / ABSENT
    }

    // ---------------- Profile 360 ----------------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffOverview {
        private Integer leaveBalance;
        private Integer responsibilityCount;
        private Integer classesAssigned;
        private Double attendancePercent;
        private String nextLeave;
        private Double yearsOfService;
        private List<KeyResponsibility> keyResponsibilities;
        private List<SubjectLoad> subjectLoad;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyResponsibility {
        private String name;
        private String scope;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectLoad {
        private String subjectName;
        private Integer periodsPerWeek;
        private Integer classCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffPersonal {
        private String firstName;
        private String middleName;
        private String lastName;
        private String gender;
        private LocalDate dateOfBirth;
        private String email;
        private String mobileNumber;
        private String address;
        private String city;
        private String state;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffEmployment {
        private String staffCode;
        private String departmentName;
        private String branchName;
        private LocalDate hireDate;
        private Double yearsOfService;
        private String employmentType;
        private String designation;
        private Boolean isActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffTeachingSnapshot {
        private String subjectsCanTeach;
        private String preferredSubjects;
        private String teachingLevels;
        private String canSubstituteFor;
        private String cannotSubstituteFor;
        private String qualification;
        private Integer experienceYears;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffLeaveSnapshot {
        private Integer totalAllowance;
        private Integer used;
        private Integer balance;
        private String nextLeave;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffPayrollSnapshot {
        private Double basic;
        private Double allowances;
        private Double deductions;
        private Double netSalary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffProfile360 {
        private Long staffId;
        private String staffCode;
        private String fullName;
        private String designation;
        private LocalDate hireDate;
        private String email;
        private String mobileNumber;
        private String photoUrl;
        private StaffOverview overview;
        private StaffPersonal personal;
        private StaffEmployment employment;
        private StaffTeachingSnapshot teaching;
        private StaffLeaveSnapshot leaveSnapshot;
        private StaffPayrollSnapshot payroll;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffTimelineEntry {
        private Long id;
        private String date;
        private String title;
        private String description;
        private String type;
    }

    // ---------------- Responsibilities ----------------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponsibilityRequest {
        @NotBlank
        private String responsibilityName;
        @NotBlank
        private String responsibilityType;
        private Long staffId;
        private String scope;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private String status;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponsibilityResponse {
        private Long responsibilityId;
        private String responsibilityName;
        private String responsibilityType;
        private Long staffId;
        private String staffName;
        private String scope;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private String status;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponsibilityKpi {
        private long total;
        private long assignedToday;
        private long unassigned;
        private long custom;
    }

    // ---------------- Leave & Availability ----------------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaveAvailabilityKpi {
        private long presentToday;
        private long onLeaveToday;
        private long absentToday;
        private long upcomingLeaves;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayLeaveEntry {
        private Long staffId;
        private String staffName;
        private String department;
        private String leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer days;
        private String reason;
        private String status;
    }

    // ---------------- Document Vault ----------------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffDocumentKpi {
        private long total;
        private long verified;
        private long pending;
        private long missing;
        private long expired;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffDocumentRequest {
        @NotNull
        private Long staffId;
        @NotBlank
        private String category;
        @NotBlank
        private String documentType;
        @NotBlank
        private String fileName;
        private String fileUrl;
        private Long fileSize;
        private String status;
        private LocalDate expiresOn;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffDocumentEntry {
        private Long documentId;
        private Long staffId;
        private String staffName;
        private String category;
        private String documentType;
        private String fileName;
        private String fileUrl;
        private Long fileSize;
        private String status;
        private String verifiedBy;
        private LocalDate verifiedOn;
        private LocalDate expiresOn;
        private String remarks;
        private String uploadedOn;
    }

    // ---------------- Alumni Staff ----------------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlumniStaffKpi {
        private long total;
        private long retired;
        private long resigned;
        private long contractCompleted;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlumniStaffRequest {
        @NotBlank
        private String fullName;
        private Long staffId;
        private String staffCode;
        private String lastDesignation;
        private String department;
        @NotBlank
        private String exitType;
        @NotNull
        private LocalDate exitDate;
        private LocalDate joinedDate;
        private String email;
        private String contact;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlumniStaffResponse {
        private Long alumniStaffId;
        private Long staffId;
        private String staffCode;
        private String fullName;
        private String lastDesignation;
        private String department;
        private String exitType;
        private LocalDate exitDate;
        private LocalDate joinedDate;
        private Double yearsOfService;
        private String email;
        private String contact;
        private String remarks;
    }

    // ---------------- Teaching Profile ----------------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeachingProfileRequest {
        @NotNull
        private Long staffId;
        private String subjectsCanTeach;
        private String preferredSubjects;
        private String teachingLevels;
        private String canSubstituteFor;
        private String cannotSubstituteFor;
        private String qualification;
        private Integer experienceYears;
        private String remarks;
    }
}
