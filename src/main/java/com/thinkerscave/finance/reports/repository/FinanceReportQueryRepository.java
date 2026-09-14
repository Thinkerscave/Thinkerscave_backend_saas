package com.thinkerscave.finance.reports.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FinanceReportQueryRepository {
    private final EntityManager entityManager;

    public Object[] kpis(LocalDate from, LocalDate to, Long yearId) {
        return row("""
            SELECT
              (SELECT COALESCE(SUM(amount),0) FROM fee_payment
                WHERE status='SUCCESS' AND CAST(paid_on AS date) BETWEEN :from AND :to),
              (SELECT COALESCE(SUM(balance_amount),0) FROM student_billing_period
                WHERE academic_year_id=:yearId AND balance_amount>0),
              (SELECT COALESCE(SUM(amount),0) FROM payroll_payment WHERE paid_on BETWEEN :from AND :to),
              (SELECT COALESCE(SUM(amount),0) FROM expense_payment WHERE paid_on BETWEEN :from AND :to)
            """, from, to, yearId);
    }

    public List<Object[]> trend(LocalDate from, LocalDate to) {
        return rows("""
            SELECT bucket, SUM(fees), SUM(outflow) FROM (
              SELECT date_trunc('month', paid_on)::date bucket, SUM(amount) fees, 0::numeric outflow
                FROM fee_payment WHERE status='SUCCESS' AND CAST(paid_on AS date) BETWEEN :from AND :to GROUP BY 1
              UNION ALL
              SELECT date_trunc('month', paid_on)::date, 0, SUM(amount)
                FROM payroll_payment WHERE paid_on BETWEEN :from AND :to GROUP BY 1
              UNION ALL
              SELECT date_trunc('month', paid_on)::date, 0, SUM(amount)
                FROM expense_payment WHERE paid_on BETWEEN :from AND :to GROUP BY 1
            ) x GROUP BY bucket ORDER BY bucket
            """, from, to, null);
    }

    public List<Object[]> feeCategories(LocalDate from, LocalDate to) {
        return rows("""
            SELECT COALESCE(NULLIF(l.fee_head_category,''), l.fee_head_name) category,
                   SUM(a.allocated_amount * l.amount / NULLIF(t.line_total,0)) amount
              FROM fee_payment p
              JOIN fee_payment_allocation a ON a.fee_payment_id=p.fee_payment_id
              JOIN student_billing_period_line l ON l.student_billing_period_id=a.student_billing_period_id
              JOIN (SELECT student_billing_period_id, SUM(amount) line_total
                      FROM student_billing_period_line GROUP BY student_billing_period_id) t
                ON t.student_billing_period_id=a.student_billing_period_id
             WHERE p.status='SUCCESS' AND CAST(p.paid_on AS date) BETWEEN :from AND :to
             GROUP BY 1 ORDER BY amount DESC
            """, from, to, null);
    }

    public Object[] outstandingSummary(Long yearId) {
        return row("""
            SELECT COALESCE(SUM(balance_amount),0), COUNT(DISTINCT student_id)
              FROM student_billing_period WHERE academic_year_id=:yearId AND balance_amount>0
            """, null, null, yearId);
    }

    public List<Object[]> outstandingByClass(Long yearId) {
        return rows("""
            SELECT class_id, COALESCE(class_name,'Unassigned'), COUNT(DISTINCT student_id),
                   COALESCE(SUM(balance_amount),0)
              FROM student_billing_period
             WHERE academic_year_id=:yearId AND balance_amount>0
             GROUP BY class_id, class_name ORDER BY SUM(balance_amount) DESC
            """, null, null, yearId);
    }

    public Object[] payrollSummary(LocalDate from, LocalDate to) {
        return row("""
            WITH scoped AS (
              SELECT ep.* FROM employee_payroll ep
               WHERE make_date(ep.payroll_year,ep.payroll_month,1)
                     BETWEEN date_trunc('month',CAST(:from AS date))::date
                         AND date_trunc('month',CAST(:to AS date))::date
            )
            SELECT COALESCE(SUM(ep.net_amount),0),
                   (SELECT COALESCE(SUM(amount),0) FROM payroll_payment WHERE paid_on BETWEEN :from AND :to),
                   COALESCE(SUM(GREATEST(ep.net_amount-COALESCE(pp.paid,0),0)),0),
                   COUNT(DISTINCT ep.staff_id)
              FROM scoped ep
              LEFT JOIN (SELECT employee_payroll_id, SUM(amount) paid FROM payroll_payment GROUP BY employee_payroll_id) pp
                ON pp.employee_payroll_id=ep.employee_payroll_id
            """, from, to, null);
    }

    public List<Object[]> payrollTrend(LocalDate from, LocalDate to) {
        return rows("""
            SELECT date_trunc('month',paid_on)::date, SUM(amount)
              FROM payroll_payment WHERE paid_on BETWEEN :from AND :to GROUP BY 1 ORDER BY 1
            """, from, to, null);
    }

    public Object[] expenseAttention() {
        return (Object[]) entityManager.createNativeQuery("""
            SELECT
              COALESCE(SUM(amount) FILTER (WHERE approval_status='PENDING_APPROVAL'),0),
              COUNT(*) FILTER (WHERE approval_status='PENDING_APPROVAL'),
              COALESCE(SUM(remaining_amount) FILTER (WHERE approval_status='APPROVED' AND payment_status<>'PAID'),0),
              COUNT(*) FILTER (WHERE approval_status='APPROVED' AND payment_status<>'PAID')
            FROM expense
            """).getSingleResult();
    }

    public List<Object[]> expenseCategories(LocalDate from, LocalDate to) {
        return rows("""
            SELECT e.category_name_snapshot, SUM(p.amount)
              FROM expense_payment p JOIN expense e ON e.expense_id=p.expense_id
             WHERE p.paid_on BETWEEN :from AND :to
             GROUP BY e.category_name_snapshot ORDER BY SUM(p.amount) DESC
            """, from, to, null);
    }

    public List<Object[]> expenseStatuses(LocalDate from, LocalDate to) {
        return rows("""
            SELECT payment_status, COUNT(*), COALESCE(SUM(amount),0)
              FROM expense WHERE approval_status='APPROVED' AND expense_date BETWEEN :from AND :to
             GROUP BY payment_status ORDER BY payment_status
            """, from, to, null);
    }

    public List<Object[]> topExpenseHeads(LocalDate from, LocalDate to) {
        return rows("""
            SELECT e.expense_head_id, e.head_name_snapshot, e.category_name_snapshot, SUM(p.amount)
              FROM expense_payment p JOIN expense e ON e.expense_id=p.expense_id
             WHERE p.paid_on BETWEEN :from AND :to
             GROUP BY e.expense_head_id,e.head_name_snapshot,e.category_name_snapshot
             ORDER BY SUM(p.amount) DESC LIMIT 10
            """, from, to, null);
    }

    public List<Object[]> recent(LocalDate from, LocalDate to, int limit) {
        Query query = entityManager.createNativeQuery("""
            SELECT type, activity_date, reference, description, amount, status, source_id FROM (
              SELECT 'FEE_COLLECTION' type, CAST(p.paid_on AS date) activity_date,
                     COALESCE(r.receipt_number,p.reference_number,'FEE-'||p.fee_payment_id) reference,
                     'Fee collection' description, p.amount, p.status, p.fee_payment_id source_id
                FROM fee_payment p LEFT JOIN fee_receipt r ON r.fee_payment_id=p.fee_payment_id
               WHERE p.status='SUCCESS' AND CAST(p.paid_on AS date) BETWEEN :from AND :to
              UNION ALL
              SELECT 'EXPENSE',p.paid_on,COALESCE(p.reference_number,e.expense_number),
                     e.head_name_snapshot,p.amount,e.payment_status,e.expense_id
                FROM expense_payment p JOIN expense e ON e.expense_id=p.expense_id
               WHERE p.paid_on BETWEEN :from AND :to
              UNION ALL
              SELECT 'PAYROLL',p.paid_on,COALESCE(p.reference_number,'PAYROLL-'||p.payroll_payment_id),
                     'Payroll payment',p.amount,'PAID',p.employee_payroll_id
                FROM payroll_payment p WHERE p.paid_on BETWEEN :from AND :to
            ) activity ORDER BY activity_date DESC, reference DESC LIMIT :limit
            """);
        query.setParameter("from", from);
        query.setParameter("to", to);
        query.setParameter("limit", limit);
        return query.getResultList();
    }

    private Object[] row(String sql, LocalDate from, LocalDate to, Long yearId) {
        return (Object[]) bind(entityManager.createNativeQuery(sql), from, to, yearId).getSingleResult();
    }

    private List<Object[]> rows(String sql, LocalDate from, LocalDate to, Long yearId) {
        return bind(entityManager.createNativeQuery(sql), from, to, yearId).getResultList();
    }

    private Query bind(Query query, LocalDate from, LocalDate to, Long yearId) {
        if (from != null) query.setParameter("from", from);
        if (to != null) query.setParameter("to", to);
        if (yearId != null) query.setParameter("yearId", yearId);
        return query;
    }
}
