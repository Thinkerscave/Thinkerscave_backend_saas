package com.thinkerscave.admission.dto.request;

import com.thinkerscave.admission.enums.FollowUpType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Counseling session record for a prospect inquiry")
public class CounselingNoteRequest {

    /** Actual date/time the session took place. Defaults to now() server-side when omitted. */
    private LocalDateTime sessionAt;

    @NotNull(message = "Counseling mode is required")
    private FollowUpType mode;

    /**
     * Staff (COUNSELOR responsibility) who conducted the session. Defaults to the
     * logged-in counselor server-side when omitted and the caller is staff.
     */
    private Long counselorStaffId;

    private String studentRequirements;
    private String parentConcerns;
    private String campusVisitInfo;
    private String recommendations;

    @NotBlank(message = "Notes are required")
    private String notes;
}
