package com.thinkerscave.student.dto;

import com.thinkerscave.student.enums.TransferStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferStatusUpdateRequest {

    @NotNull
    private TransferStatus status;

    private String remarks;
}
