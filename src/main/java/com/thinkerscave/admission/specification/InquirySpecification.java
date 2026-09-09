package com.thinkerscave.admission.specification;

import com.thinkerscave.admission.dto.request.LeadSearchRequest;
import com.thinkerscave.admission.entity.Inquiry;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class InquirySpecification {

    private InquirySpecification() {
    }

    public static Specification<Inquiry> filter(LeadSearchRequest request) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            // Schema-per-tenant: Automatically scoped to current tenant schema
            predicates.add(cb.isFalse(root.get("deleted")));

            if (request == null) {
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (request.getSource() != null) {
                predicates.add(cb.equal(root.get("inquirySource"), request.getSource()));
            }

            if (hasText(request.getClassInterestedIn())) {
                predicates.add(cb.equal(cb.lower(root.get("classInterestedIn")), request.getClassInterestedIn().trim().toLowerCase()));
            }

            if (request.getAcademicYearId() != null) {
                predicates.add(cb.equal(root.get("academicYearId"), request.getAcademicYearId()));
            }

            if (request.getClassId() != null) {
                predicates.add(cb.equal(root.get("classId"), request.getClassId()));
            }

            if (request.getCounselorId() != null) {
                predicates.add(cb.equal(root.get("assignedCounselorId"), request.getCounselorId()));
            }

            if (request.getFollowUpFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("nextFollowUpDate"), request.getFollowUpFrom()));
            }

            if (request.getFollowUpTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("nextFollowUpDate"), request.getFollowUpTo()));
            }

            if (hasText(request.getCreatedBy())) {
                predicates.add(cb.equal(cb.lower(root.get("createdBy")), request.getCreatedBy().trim().toLowerCase()));
            }

            if (hasText(request.getKeyword())) {
                String like = "%" + request.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("mobileNumber")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("email"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("inquiryNumber"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("comments"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("referredBy"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("studentName"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("parentContactName"), "")), like)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}