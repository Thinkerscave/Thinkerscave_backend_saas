package com.thinkerscave.academics.dto.request;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SubjectChoiceMappingSaveRequest {
    private List<@Valid SubjectChoiceMappingUpsertRequest> upserts = new ArrayList<>();
    private List<Long> removedChoiceGroupIds = new ArrayList<>();
    private boolean confirmImpact;
}
