package com.thinkerscave.staff.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkResponsibilityAssignmentRequest {

    @NotEmpty(message = "Select at least one staff member")
    private List<Long> staffIds;
}
