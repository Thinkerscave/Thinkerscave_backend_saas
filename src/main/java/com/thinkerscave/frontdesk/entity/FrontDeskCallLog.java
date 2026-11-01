package com.thinkerscave.frontdesk.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "front_desk_call_log")
public class FrontDeskCallLog extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "call_id")
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "caller_name", nullable = false, length = 150)
    private String callerName;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "call_type", nullable = false, length = 20)
    private String callType;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "purpose", length = 300)
    private String purpose;

    @Column(name = "action_taken", length = 500)
    private String actionTaken;

    @Column(name = "forwarded_to", length = 200)
    private String forwardedTo;
}
