package com.thinkerscave.academics.timetable.engine;

public enum GenerationPhase {

    CHECKING_SETUP(1, "Checking academic setup"),
    PREPARING_WORKSPACE(2, "Preparing timetable workspace"),
    LOADING_DATA(3, "Loading classes, subjects and teachers"),
    BUILDING_REQUIREMENTS(4, "Preparing teaching requirements"),
    CALCULATING_CANDIDATES(5, "Finding suitable time slots"),
    SCHEDULING(6, "Solving the most important placements"),
    OPTIMIZING(7, "Balancing classes, teachers and subjects"),
    VALIDATING(8, "Running final consistency checks"),
    GENERATING_CONFLICTS(9, "Checking for remaining issues"),
    FINALIZING(10, "Preparing timetable for review");

    private final int phaseNumber;
    private final String label;

    GenerationPhase(int phaseNumber, String label) {
        this.phaseNumber = phaseNumber;
        this.label = label;
    }

    public int getPhaseNumber() {
        return phaseNumber;
    }

    public String getLabel() {
        return label;
    }
}
