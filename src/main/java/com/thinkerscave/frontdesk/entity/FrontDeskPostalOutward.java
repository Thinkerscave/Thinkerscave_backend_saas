package com.thinkerscave.frontdesk.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;


@Entity
@Getter
@Setter
@Table(name = "front_desk_postal_outward")
public class FrontDeskPostalOutward extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outward_id")
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Column(name = "destination_address", nullable = false, length = 500)
    private String destinationAddress;

    @Column(name = "document_type", length = 80)
    private String documentType;

    @Column(name = "dispatch_mode", length = 40)
    private String dispatchMode;

    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

    @Column(name = "tracking_number", length = 80)
    private String trackingNumber;

    @Column(name = "sender_reference", length = 80)
    private String senderReference;
}
