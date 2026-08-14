package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.TimetableSlotKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class TimetablePeriodRequest {

    @NotNull
    @Positive
    private Short periodNumber;

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @NotNull
    private TimetableSlotKind slotKind;
}
