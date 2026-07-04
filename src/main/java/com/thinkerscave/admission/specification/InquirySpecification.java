package com.thinkerscave.admission.specification;

import com.thinkerscave.admission.dto.request.LeadSearchRequest;
import com.thinkerscave.admission.entity.Inquiry;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class InquirySpecification {

    private InquirySpecification() {
    }

    public static Specification<Inquiry> filter(Long organizationId, LeadSearchRequest request) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));
            predicates.add(cb.isFalse(root.get("deleted")));

            if (request == null) {
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (hasText(request.getSource())) {
                predicates.add(cb.equal(cb.lower(root.get("inquirySource")), request.getSource().trim().toLowerCase()));
            }

            if (hasText(request.getClassInterestedIn())) {
                predicates.add(cb.equal(cb.lower(root.get("classInterestedIn")), request.getClassInterestedIn().trim().toLowerCase()));
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

            if (hasText(request.getKeyword())) {
                String like = "%" + request.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("mobileNumber")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("comments")), like),
                        cb.like(cb.lower(root.get("referredBy")), like)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}