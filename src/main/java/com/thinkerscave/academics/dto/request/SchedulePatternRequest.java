package com.thinkerscave.academics.dto.request;

import com.thinkerscave.academics.enums.TimetableSlotKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SchedulePatternRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private Boolean active = true;

    @Valid
    @NotEmpty
    private List<Slot> slots = new ArrayList<>();

    @Getter
    @Setter
    public static class Slot {
        @NotBlank
        @Size(max = 50)
        private String name;

        @NotNull
        private TimetableSlotKind slotType;

        @NotNull
        private LocalTime startTime;

        @NotNull
        private LocalTime endTime;
    }
}
