package com.thinkerscave.admission.specification;

import com.thinkerscave.admission.dto.request.ApplicationSearchRequest;
import com.thinkerscave.admission.entity.ApplicationAdmission;
import com.thinkerscave.admission.entity.Inquiry;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ApplicationAdmissionSpecification {

    private ApplicationAdmissionSpecification() {
    }

    public static Specification<ApplicationAdmission> filter(ApplicationSearchRequest request) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            // Schema-per-tenant: Automatically scoped to current tenant schema

            if (request == null) {
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            }

            if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(request.getStatuses()));
            } else if (request.getStatus() != null) {
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

            boolean mineByCreator = hasText(request.getCreatedBy());
            boolean mineByCounselor = request.getAssignedCounselorId() != null;
            if (mineByCreator || mineByCounselor) {
                List<jakarta.persistence.criteria.Predicate> ownership = new ArrayList<>();
                if (mineByCreator) {
                    ownership.add(cb.equal(
                            cb.lower(root.get("createdBy")),
                            request.getCreatedBy().trim().toLowerCase()));
                }
                if (mineByCounselor) {
                    var inquiryIds = query.subquery(Long.class);
                    var inquiryRoot = inquiryIds.from(Inquiry.class);
                    inquiryIds.select(inquiryRoot.get("inquiryId"))
                            .where(cb.equal(inquiryRoot.get("assignedCounselorId"), request.getAssignedCounselorId()));
                    ownership.add(root.get("inquiryId").in(inquiryIds));
                }
                predicates.add(cb.or(ownership.toArray(new jakarta.persistence.criteria.Predicate[0])));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
