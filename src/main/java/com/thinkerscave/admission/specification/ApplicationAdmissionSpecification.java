package com.thinkerscave.admission.specification;

import com.thinkerscave.admission.dto.request.ApplicationSearchRequest;
import com.thinkerscave.admission.entity.ApplicationAdmission;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ApplicationAdmissionSpecification {

    private ApplicationAdmissionSpecification() {
    }

    public static Specification<ApplicationAdmission> filter(Long organizationId, ApplicationSearchRequest request) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (request == null) {
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (hasText(request.getApplyingForClass())) {
                predicates.add(cb.equal(cb.lower(root.get("applyingForClass")), request.getApplyingForClass().trim().toLowerCase()));
            }

            if (hasText(request.getKeyword())) {
                String like = "%" + request.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("applicationNumber")), like),
                        cb.like(cb.lower(root.get("applicantName")), like),
                        cb.like(cb.lower(root.get("contactNumber")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("parentName")), like)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}