package com.thinkerscave.access.specification;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.enums.RoleType;
import com.thinkerscave.access.enums.UserStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> filter(Long organizationId, UserStatus status, RoleType roleType, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (roleType != null) {
                // Join with userRoles → role
                Join<Object, Object> userRolesJoin = root.join("userRoles", JoinType.INNER);
                Join<Object, Object> roleJoin = userRolesJoin.join("role", JoinType.INNER);
                predicates.add(cb.equal(roleJoin.get("roleType"), roleType));
                predicates.add(cb.equal(userRolesJoin.get("active"), true));
            }

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("displayName")), pattern),
                        cb.like(cb.lower(root.get("userCode")), pattern)
                ));
            }

            if (query.getResultType() != Long.class) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
