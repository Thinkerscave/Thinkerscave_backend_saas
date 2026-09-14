package com.thinkerscave.finance.expense.entity;

import com.thinkerscave.finance.expense.enums.ExpenseHeadStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "expense_head")
public class ExpenseHead extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_head_id")
    @EqualsAndHashCode.Include
    private Long expenseHeadId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_category_id", nullable = false)
    private ExpenseCategory category;

    @Column(name = "default_requester_staff_id")
    private Long defaultRequesterStaffId;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExpenseHeadStatus status = ExpenseHeadStatus.ACTIVE;
}
