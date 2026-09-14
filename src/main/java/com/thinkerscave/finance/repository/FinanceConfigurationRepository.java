package com.thinkerscave.finance.repository;

import com.thinkerscave.finance.entity.FinanceConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceConfigurationRepository extends JpaRepository<FinanceConfiguration, Long> {
}
