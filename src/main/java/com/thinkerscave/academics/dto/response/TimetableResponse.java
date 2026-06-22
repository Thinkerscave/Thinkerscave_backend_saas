package com.thinkerscave.academics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimetableResponse {
    private Long classId;
    private String className;
    private Long sectionId;
    private String sectionName;
    /** day -> ordered list of slots */
    private Map<String, List<TimetableSlotResponse>> schedule;
}
