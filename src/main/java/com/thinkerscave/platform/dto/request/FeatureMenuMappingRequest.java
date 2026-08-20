package com.thinkerscave.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FeatureMenuMappingRequest {

    @NotNull
    private List<Long> menuIds = new ArrayList<>();
}
