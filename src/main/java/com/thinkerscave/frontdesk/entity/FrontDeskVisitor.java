package com.thinkerscave.frontdesk.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@Table(name = "front_desk_visitor")
public class FrontDeskVisitor extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visitor_id")
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "visitor_name", nullable = false, length = 150)
    private String visitorName;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "id_type", length = 40)
    private String idType;

    @Column(name = "id_number", length = 80)
    private String idNumber;

    @Column(name = "purpose", nullable = false, length = 30)
    private String purpose;

    @Column(name = "meet_person_type", length = 20)
    private String meetPersonType;

    @Column(name = "meet_person_id")
    private Long meetPersonId;

    @Column(name = "meet_person_name", length = 200)
    private String meetPersonName;

    @Column(name = "vehicle_number", length = 40)
    private String vehicleNumber;

    @Column(name = "badge_number", length = 40)
    private String badgeNumber;

    @Column(name = "in_time", nullable = false)
    private LocalDateTime inTime;

    @Column(name = "out_time")
    private LocalDateTime outTime;

    @Column(name = "photo_path", length = 1000)
    private String photoPath;

    @Transient
    private Boolean overstay;
}
