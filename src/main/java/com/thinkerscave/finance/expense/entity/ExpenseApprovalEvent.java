package com.thinkerscave.finance.expense.entity;

import com.thinkerscave.finance.expense.enums.ExpenseApprovalEventType;
import com.thinkerscave.finance.expense.enums.ExpenseApprovalStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "expense_approval_event")
public class ExpenseApprovalEvent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_approval_event_id")
    @EqualsAndHashCode.Include
    private Long expenseApprovalEventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private ExpenseApprovalEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private ExpenseApprovalStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private ExpenseApprovalStatus toStatus;

    @Column(name = "actor", nullable = false, length = 100)
    private String actor;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "occurred_on", nullable = false)
    private Instant occurredOn = Instant.now();
}
