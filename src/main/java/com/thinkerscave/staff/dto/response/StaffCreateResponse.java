package com.thinkerscave.staff.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StaffCreateResponse {

    private Long staffId;
    private String staffCode;
    private Long userId;
}
