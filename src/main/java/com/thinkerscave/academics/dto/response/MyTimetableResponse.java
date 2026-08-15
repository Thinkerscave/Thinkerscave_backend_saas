package com.thinkerscave.academics.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyTimetableResponse {

    private String role;
    private Long academicYearId;
    private TimetableGridResponse grid;
    private List<TodayEntry> todaySchedule;
    private String message;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TodayEntry {
        private int periodNumber;
        private String periodLabel;
        private String startTime;
        private String endTime;
        private String subjectName;
        private String className;
        private String sectionName;
        private String roomName;
    }
}
