package com.thinkerscave.common.student.workspace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class StudentProfile360Response {
    private Overview overview;
    private Personal personal;
    private Family family;
    private Academics academics;
    private AttendanceSnapshot attendance;
    private FeeSnapshot fees;
    private MedicalSnapshot medical;

    @Data @Builder
    public static class Overview {
        private Long studentId;
        private String admissionNumber;
        private String rollNumber;
        private String fullName;
        private String className;
        private String sectionName;
        private String gender;
        private LocalDate dateOfBirth;
        private Integer ageYears;
        private String mobile;
        private String email;
        private String photoUrl;
        private String academicYear;
        private LocalDate admissionDate;
        private Boolean active;
        private String bloodGroup;
        private String motherTongue;
        private String nationality;
        private String religion;
        private String house;
        private String transport;
    }

    @Data @Builder
    public static class Personal {
        private String fullName;
        private String gender;
        private LocalDate dateOfBirth;
        private String nationality;
        private String religion;
        private String bloodGroup;
        private String motherTongue;
        private String permanentAddress;
        private String currentAddress;
        private String remarks;
    }

    @Data @Builder
    public static class Family {
        private GuardianInfo primary;
        private java.util.List<GuardianInfo> guardians;
        private java.util.List<SiblingInfo> siblings;
    }

    @Data @Builder
    public static class GuardianInfo {
        private Long guardianId;
        private String name;
        private String relation;
        private String email;
        private String mobile;
        private String address;
        private String occupation;
    }

    @Data @Builder
    public static class SiblingInfo {
        private Long studentId;
        private String name;
        private String relationship;
        private String className;
        private String sectionName;
        private Boolean active;
    }

    @Data @Builder
    public static class Academics {
        private String currentClass;
        private String currentSection;
        private String rollNumber;
        private String academicYear;
        private LocalDate admissionDate;
        private Long admissionAgeYears;
        private long courseCount;
        private long subjectCount;
    }

    @Data @Builder
    public static class AttendanceSnapshot {
        private int totalWorkingDays;
        private int present;
        private int absent;
        private int late;
        private int percent;
    }

    @Data @Builder
    public static class FeeSnapshot {
        private double totalFee;
        private double paid;
        private double pending;
        private String status;
    }

    @Data @Builder
    public static class MedicalSnapshot {
        private String bloodGroup;
        private String allergies;
        private String medications;
        private String emergencyContact;
        private String notes;
    }
}
