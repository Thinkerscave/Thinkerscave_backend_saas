package com.thinkerscave.academics.timetable.engine;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class GenerationProgressTracker {

    private final ConcurrentHashMap<Long, GenerationProgress> progressMap = new ConcurrentHashMap<>();

    public GenerationProgress init(Long generationId) {
        GenerationProgress progress = new GenerationProgress(generationId);
        progressMap.put(generationId, progress);
        return progress;
    }

    public GenerationProgress get(Long generationId) {
        return progressMap.get(generationId);
    }

    public void remove(Long generationId) {
        progressMap.remove(generationId);
    }

    public boolean hasActive(Long generationId) {
        GenerationProgress p = progressMap.get(generationId);
        return p != null && !p.isTerminal();
    }
}
