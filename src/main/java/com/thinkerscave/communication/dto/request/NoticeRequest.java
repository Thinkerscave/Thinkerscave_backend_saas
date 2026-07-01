package com.thinkerscave.communication.dto.request;

import com.thinkerscave.communication.enums.NoticeAudienceType;
import com.thinkerscave.communication.enums.NoticeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Create or update a notice")
public class NoticeRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String content;
    private String category;
    private boolean pinned = false;
    private LocalDate publishDate;
    private LocalDate expiryDate;
    private NoticeStatus status = NoticeStatus.DRAFT;
    private String attachmentUrl;

    @Schema(description = "Audience segments for this notice")
    private List<AudienceEntry> audiences;

    @Data
    public static class AudienceEntry {
        private NoticeAudienceType audienceType;
        private Long refId;
    }
}
