package com.thinkerscave.student.entity;

import com.thinkerscave.shared.entity.Auditable;
import com.thinkerscave.student.enums.ParentRelationship;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "student_parent",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_parent_relation",
                        columnNames = {
                                "student_id",
                                "parent_id",
                                "relationship"
                        }
                )
        }
)
public class StudentParent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_parent_id")
    @EqualsAndHashCode.Include
    private Long studentParentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship", nullable = false, length = 30)
    private ParentRelationship relationship;

    /**
     * Primary contact for school communication.
     */
    @Column(name = "primary_contact")
    private Boolean primaryContact = false;

    /**
     * Receives SMS.
     */
    @Column(name = "receive_sms")
    private Boolean receiveSms = true;

    /**
     * Receives Email.
     */
    @Column(name = "receive_email")
    private Boolean receiveEmail = true;

    /**
     * Can pick up student.
     */
    @Column(name = "pickup_authorized")
    private Boolean pickupAuthorized = true;

    @Column(name = "active")
    private Boolean active = true;
}