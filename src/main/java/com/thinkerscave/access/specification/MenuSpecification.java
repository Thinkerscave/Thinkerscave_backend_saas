package com.thinkerscave.access.specification;

import com.thinkerscave.access.entity.Menu;
import com.thinkerscave.access.enums.MenuType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class MenuSpecification {

    private MenuSpecification() {}

    public static Specification<Menu> filter(MenuType menuType, Boolean active, Boolean showInSidebar, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (menuType != null) {
                predicates.add(cb.equal(root.get("menuType"), menuType));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (showInSidebar != null) {
                predicates.add(cb.equal(root.get("showInSidebar"), showInSidebar));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("menuCode")), pattern),
                        cb.like(cb.lower(root.get("menuName")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
