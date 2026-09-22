package com.thinkerscave.finance.repository;

import com.thinkerscave.finance.entity.FeePaymentMethod;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeePaymentMethodRepository extends JpaRepository<FeePaymentMethod, Long> {

    Optional<FeePaymentMethod> findByNameNormalized(String nameNormalized);

    boolean existsByNameNormalized(String nameNormalized);

    List<FeePaymentMethod> findByStatusOrderBySortOrderAscNameAsc(FeeMasterStatus status);

    List<FeePaymentMethod> findAllByOrderBySortOrderAscNameAsc();
}
