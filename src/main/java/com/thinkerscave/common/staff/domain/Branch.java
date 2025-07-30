package com.thinkerscave.common.staff.domain;



import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "branch")
public class Branch  extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id")
    private Long id;

    @Column(name = "branch_name", nullable = false, unique = true, length = 255)
    private String branchName;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "is_active")
    private Boolean isActive = true;
}

