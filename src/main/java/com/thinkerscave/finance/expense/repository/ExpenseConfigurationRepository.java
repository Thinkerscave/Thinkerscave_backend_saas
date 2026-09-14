package com.thinkerscave.finance.expense.repository;
import com.thinkerscave.finance.expense.entity.ExpenseConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ExpenseConfigurationRepository extends JpaRepository<ExpenseConfiguration,Long> {}
