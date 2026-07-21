package com.thinkerscave.shared.entity;

import com.thinkerscave.shared.enums.CodeType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "code_sequence")
public class CodeSequence extends Auditable {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "code_type", length = 50)
    private CodeType codeType;

    @Column(name = "last_value", nullable = false)
    private Long lastValue = 0L;
}
