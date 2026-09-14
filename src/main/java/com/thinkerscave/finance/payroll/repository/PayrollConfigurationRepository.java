package com.thinkerscave.finance.payroll.repository;

import com.thinkerscave.finance.payroll.entity.PayrollConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayrollConfigurationRepository extends JpaRepository<PayrollConfiguration, Long> {
    Optional<PayrollConfiguration> findFirstByOrderByPayrollConfigurationIdAsc();
}
