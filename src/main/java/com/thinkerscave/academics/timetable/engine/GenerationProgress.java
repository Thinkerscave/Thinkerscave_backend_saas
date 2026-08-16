package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.TimetableGenerationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerationProgress {

    private Long generationId;
    private GenerationPhase phase;
    private int progressPercent;
    private TimetableGenerationStatus terminalStatus;
    private String message;
    private volatile boolean cancelRequested;

    public GenerationProgress(Long generationId) {
        this.generationId = generationId;
        this.phase = GenerationPhase.CHECKING_SETUP;
        this.progressPercent = 0;
    }

    public void advanceTo(GenerationPhase phase, int percent) {
        this.phase = phase;
        this.progressPercent = Math.min(percent, 100);
    }

    public void complete(TimetableGenerationStatus status, String message) {
        this.terminalStatus = status;
        this.progressPercent = 100;
        this.message = message;
    }

    public boolean isTerminal() {
        return terminalStatus != null;
    }

    public void requestCancel() {
        this.cancelRequested = true;
    }
}
