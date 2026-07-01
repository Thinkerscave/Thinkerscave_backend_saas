package com.thinkerscave.communication.dto.response;

import com.thinkerscave.communication.enums.NoticeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Notice response")
public class NoticeResponse {

    private Long noticeId;
    private String title;
    private String content;
    private String category;
    private boolean pinned;
    private LocalDate publishDate;
    private LocalDate expiryDate;
    private NoticeStatus status;
    private String attachmentUrl;
    private Long publishedByUserId;
    private LocalDateTime createdOn;
    private String createdBy;
    private List<AudienceEntryResponse> audiences;

    @Data
    @Builder
    public static class AudienceEntryResponse {
        private Long audienceId;
        private String audienceType;
        private Long refId;
    }
}
