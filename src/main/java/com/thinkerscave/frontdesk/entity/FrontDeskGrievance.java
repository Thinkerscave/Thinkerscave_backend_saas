package com.thinkerscave.frontdesk.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "front_desk_grievance")
public class FrontDeskGrievance extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grievance_id")
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "complainant_type", nullable = false, length = 20)
    private String complainantType;

    @Column(name = "complainant_name", length = 150)
    private String complainantName;

    @Column(name = "linked_id")
    private Long linkedId;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "assignee_name", length = 200)
    private String assigneeName;

    @Column(name = "status", nullable = false, length = 30)
    private String status;
}
