package com.thinkerscave.communication.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Start a new message thread")
public class MessageThreadRequest {

    private String subject;

    @NotEmpty(message = "At least one participant is required")
    private List<Long> participantUserIds;
}
