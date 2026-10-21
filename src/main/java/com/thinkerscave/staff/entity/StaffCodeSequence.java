package com.thinkerscave.staff.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Per-tenant STAFF code counter (single row, id = 1). Locked while a code is issued;
 * the value only increases, so issued numbers are never reused.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "staff_code_sequence")
public class StaffCodeSequence {

    public static final int SINGLETON_ID = 1;

    @Id
    @Column(name = "id", nullable = false)
    private Integer id = SINGLETON_ID;

    @Column(name = "last_value", nullable = false)
    private Long lastValue = 0L;

    @Column(name = "updated_on", nullable = false)
    private LocalDateTime updatedOn;
}
