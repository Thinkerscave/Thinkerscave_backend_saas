package com.thinkerscave.frontdesk.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@Table(name = "front_desk_appointment")
public class FrontDeskAppointment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id")
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "visitor_name", nullable = false, length = 150)
    private String visitorName;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "staff_name", length = 200)
    private String staffName;

    @Column(name = "requested_slot", nullable = false)
    private LocalDateTime requestedSlot;

    @Column(name = "agenda", length = 500)
    private String agenda;

    @Column(name = "status", nullable = false, length = 30)
    private String status;
}
