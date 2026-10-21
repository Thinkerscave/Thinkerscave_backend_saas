package com.thinkerscave.academics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Options for the timetable grid selectors: classes with their sections, teachers and rooms. */
@Getter
@AllArgsConstructor
public class TimetableFiltersResponse {

    private List<ClassOption> classes;
    private List<Option> teachers;
    private List<Option> rooms;

    @Getter
    @AllArgsConstructor
    public static class ClassOption {
        private Long classId;
        private String name;
        private List<Option> sections;
    }

    @Getter
    @AllArgsConstructor
    public static class Option {
        private Long id;
        private String name;
    }
}
