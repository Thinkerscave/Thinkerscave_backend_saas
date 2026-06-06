package com.thinkerscave.common.subscription.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "subscription_plan")
public class SubscriptionPlan extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "plan_code", unique = true, nullable = false, length = 50)
    private String planCode;

    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "monthly_price", precision = 12, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "annual_price", precision = 12, scale = 2)
    private BigDecimal annualPrice;

    @Column(name = "currency", length = 8)
    private String currency = "INR";

    @Column(name = "max_students")
    private Integer maxStudents;

    @Column(name = "max_staff")
    private Integer maxStaff;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "storage_gb")
    private Integer storageGb;

    @Column(name = "modules_included", length = 1000)
    private String modulesIncluded;

    @Column(name = "support_tier", length = 50)
    private String supportTier;

    @Column(name = "highlight_color", length = 20)
    private String highlightColor;

    @Column(name = "is_featured")
    private Boolean featured = false;

    @Column(name = "is_active")
    private Boolean active = true;
}
