package com.thinkerscave.common.staff.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Table(name = "branch")
public class Branch extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id")
    private Long id;

    @Column(name = "branch_name", nullable = false, unique = true, length = 255)
    private String branchName;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "branch_code", unique = true, nullable = false)
    private String branchCode;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "organization_id")
    private Long organizationId;
}
