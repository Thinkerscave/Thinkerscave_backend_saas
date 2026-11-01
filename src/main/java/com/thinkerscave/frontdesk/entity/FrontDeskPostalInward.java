package com.thinkerscave.frontdesk.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@Table(name = "front_desk_postal_inward")
public class FrontDeskPostalInward extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inward_id")
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "sender_name", nullable = false, length = 150)
    private String senderName;

    @Column(name = "tracking_number", length = 80)
    private String trackingNumber;

    @Column(name = "courier_partner", length = 120)
    private String courierPartner;

    @Column(name = "addressed_to", nullable = false, length = 200)
    private String addressedTo;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status;
}
