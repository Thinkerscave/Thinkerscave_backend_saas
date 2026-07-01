package com.thinkerscave.communication.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Send a message in a thread")
public class MessageRequest {

    @NotBlank(message = "Message body is required")
    private String body;

    private String attachmentUrl;
}
