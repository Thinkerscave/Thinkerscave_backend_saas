package com.thinkerscave.staff.entity;

import com.thinkerscave.staff.enums.StaffActivityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "staff_activity_log")
@Getter
@Setter
@NoArgsConstructor
public class StaffActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long activityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private StaffActivityType actionType;

    @Column(name = "section", length = 60)
    private String section;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "performed_by", length = 255)
    private String performedBy;

    @Column(name = "performed_on", nullable = false)
    private LocalDateTime performedOn;
}
