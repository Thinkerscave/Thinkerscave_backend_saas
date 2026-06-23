package com.thinkerscave.staff.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkMarkPaidRequest {

    @NotEmpty(message = "Payroll IDs are required")
    private List<Long> payrollIds;
}
