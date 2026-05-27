package com.thinkerscave.common.communication.dto;

import com.thinkerscave.common.communication.domain.NoticeStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeDTO {
    private Long id;

    @NotBlank
    @Size(max = 256)
    private String title;

    @NotBlank
    private String content;

    private String category;
    private boolean pinned;

    @NotNull
    private LocalDate publishDate;

    private LocalDate expiryDate;
    private NoticeStatus status;
    private String attachmentUrl;
    private Long publishedByUserId;

    @Valid
    @Builder.Default
    private List<NoticeAudienceDTO> audiences = new ArrayList<>();
}
