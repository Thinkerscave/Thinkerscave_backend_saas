package com.thinkerscave.admission.specification;

import com.thinkerscave.admission.dto.request.AdmissionReportFilterRequest;
import com.thinkerscave.admission.entity.ApplicationAdmission;
import com.thinkerscave.admission.entity.Inquiry;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class AdmissionReportSpecification {

    private AdmissionReportSpecification() {
    }

    public static Specification<Inquiry> inquiries(AdmissionReportFilterRequest request) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (request == null) {
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            }
            if (request.getAcademicYearId() != null) {
                predicates.add(cb.equal(root.get("academicYearId"), request.getAcademicYearId()));
            }
            if (request.getClassId() != null) {
                predicates.add(cb.equal(root.get("classId"), request.getClassId()));
            }
            if (request.getSource() != null) {
                predicates.add(cb.equal(root.get("inquirySource"), request.getSource()));
            }
            if (request.getCounselorId() != null) {
                predicates.add(cb.equal(root.get("assignedCounselorId"), request.getCounselorId()));
            }
            if (request.getLeadStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getLeadStatus()));
            }
            addCreatedOnRange(predicates, cb, root.get("createdOn"), request.getDateFrom(), request.getDateTo());
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /**
     * Applications matching year/class/status/date, optionally restricted to inquiry IDs
     * when lead-side filters (source/counselor/lead status) are active.
     */
    public static Specification<ApplicationAdmission> applications(
            AdmissionReportFilterRequest request,
            Set<Long> inquiryIdsWhenLeadFiltered,
            boolean leadFiltersActive) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("archived")));
            if (request != null) {
                if (request.getAcademicYearId() != null) {
                    predicates.add(cb.equal(root.get("academicYearId"), request.getAcademicYearId()));
                }
                if (request.getClassId() != null) {
                    predicates.add(cb.equal(root.get("classId"), request.getClassId()));
                }
                if (request.getApplicationStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), request.getApplicationStatus()));
                }
                addCreatedOnRange(predicates, cb, root.get("createdOn"), request.getDateFrom(), request.getDateTo());
            }
            if (leadFiltersActive) {
                if (inquiryIdsWhenLeadFiltered == null || inquiryIdsWhenLeadFiltered.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("inquiryId").in(inquiryIdsWhenLeadFiltered));
                }
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private static void addCreatedOnRange(
            List<jakarta.persistence.criteria.Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Path<LocalDateTime> path,
            LocalDate from,
            LocalDate to) {
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(path, from.atStartOfDay()));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(path, to.atTime(LocalTime.MAX)));
        }
    }

    public static boolean leadFiltersActive(AdmissionReportFilterRequest request) {
        if (request == null) {
            return false;
        }
        return request.getSource() != null
                || request.getCounselorId() != null
                || request.getLeadStatus() != null;
    }
}
