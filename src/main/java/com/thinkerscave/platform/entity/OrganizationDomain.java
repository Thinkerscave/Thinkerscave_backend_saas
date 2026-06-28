package com.thinkerscave.platform.entity;

import com.thinkerscave.platform.enums.DomainStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "organization_domains",
        indexes = {
                @Index(name = "idx_org_domain", columnList = "domain"),
                @Index(name = "idx_org_subdomain", columnList = "sub_domain"),
                @Index(name = "idx_org_custom_domain", columnList = "custom_domain"),
                @Index(name = "idx_org_domain_status", columnList = "status")
        }
)
public class OrganizationDomain extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Organization.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    /**
     * ThinkersCave generated subdomain.
     * Example:
     * aakashblr.thinkerscave.app
     */
    @Column(name = "sub_domain", nullable = false, unique = true, length = 150)
    private String subDomain;

    /**
     * Complete default domain.
     */
    @Column(name = "domain", nullable = false, unique = true, length = 255)
    private String domain;

    /**
     * Customer custom domain.
     * Example:
     * erp.aakash.com
     */
    @Column(name = "custom_domain", unique = true, length = 255)
    private String customDomain;

    /**
     * SSL Enabled.
     */
    @Builder.Default
    @Column(name = "ssl_enabled")
    private Boolean sslEnabled = false;

    /**
     * SSL Certificate Provider.
     */
    @Column(name = "ssl_provider", length = 100)
    private String sslProvider;

    /**
     * SSL Expiry Date.
     */
    @Column(name = "ssl_expiry")
    private java.time.LocalDate sslExpiry;

    /**
     * DNS Verified.
     */
    @Builder.Default
    @Column(name = "dns_verified")
    private Boolean dnsVerified = false;

    /**
     * Domain Verification Token.
     */
    @Column(name = "verification_token", length = 255)
    private String verificationToken;

    /**
     * Default Domain.
     */
    @Builder.Default
    @Column(name = "default_domain")
    private Boolean defaultDomain = true;

    /**
     * Primary Domain.
     */
    @Builder.Default
    @Column(name = "primary_domain")
    private Boolean primaryDomain = true;

    /**
     * Domain Status.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DomainStatus status = DomainStatus.ACTIVE;

    /**
     * Active Flag.
     */
    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;

    /**
     * Remarks.
     */
    @Column(name = "remarks", length = 1000)
    private String remarks;

}