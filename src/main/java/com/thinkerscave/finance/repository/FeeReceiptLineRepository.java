package com.thinkerscave.finance.repository;

import com.thinkerscave.finance.entity.FeeReceiptLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeReceiptLineRepository extends JpaRepository<FeeReceiptLine, Long> {

    List<FeeReceiptLine> findByFeeReceiptIdOrderByFeeReceiptLineIdAsc(Long feeReceiptId);
}
