package com.thinkerscave.common.communication.dto;

import com.thinkerscave.common.communication.domain.NoticeAudienceType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeAudienceDTO {
    private Long id;

    @NotNull
    private NoticeAudienceType audienceType;

    private Long refId;
}
