package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class TimetableGenerationWorker {

    private static final Logger log = LoggerFactory.getLogger(TimetableGenerationWorker.class);

    private final TimetableGenerationEngine engine;
    private final GenerationProgressTracker tracker;
    private final com.thinkerscave.academics.repository.TimetableGenerationRunRepository generationRunRepository;

    public TimetableGenerationWorker(TimetableGenerationEngine engine,
                                     GenerationProgressTracker tracker,
                                     com.thinkerscave.academics.repository.TimetableGenerationRunRepository generationRunRepository) {
        this.engine = engine;
        this.tracker = tracker;
        this.generationRunRepository = generationRunRepository;
    }

    @Async("timetableGenerationExecutor")
    public void runGeneration(Long versionId, long seed, Long generationId,
                              String tenant, Long organizationId, Long userId) {
        runGeneration(versionId, seed, generationId, tenant, organizationId, userId, null);
    }

    @Async("timetableGenerationExecutor")
    public void runGeneration(Long versionId, long seed, Long generationId,
                              String tenant, Long organizationId, Long userId,
                              java.time.LocalDate referenceDate) {
        try {
            TenantContext.setTenant(tenant);
            OrganizationContext.setOrganizationId(organizationId);

            log.info("Generation worker started: generationId={}, versionId={}, tenant={}, orgId={}",
                    generationId, versionId, tenant, organizationId);

            GenerationProgress progress = tracker.get(generationId);
            if (progress == null) {
                progress = tracker.init(generationId);
            }

            var result = engine.execute(versionId, seed, progress, referenceDate);
            updateRun(versionId, result);
            log.info("Generation worker completed: generationId={}", generationId);
        } catch (Exception e) {
            log.error("Generation worker failed: generationId={}", generationId, e);
            GenerationProgress progress = tracker.get(generationId);
            if (progress != null && !progress.isTerminal()) {
                progress.complete(com.thinkerscave.academics.enums.TimetableGenerationStatus.FAILED,
                        "Unexpected error: " + e.getMessage());
            }
        } finally {
            TenantContext.clear();
            OrganizationContext.clear();
        }
    }

    private void updateRun(Long versionId, com.thinkerscave.academics.dto.response.TimetableGenerateResultResponse result) {
        try {
            generationRunRepository
                    .findTopByTimetableVersion_TimetableVersionIdOrderByGenerationRunIdDesc(versionId)
                    .ifPresent(run -> {
                        run.setFinishedAt(java.time.LocalDateTime.now());
                        run.setReport(result.getMessage());
                        if (result.getResultKind() == com.thinkerscave.academics.dto.response
                                .TimetableGenerateResultResponse.ResultKind.FAILED) {
                            run.setStatus(com.thinkerscave.academics.enums.TimetableGenerationRunStatus.FAILED);
                        } else if (result.getOpenBlockingConflicts() > 0
                                || result.getResultKind() == com.thinkerscave.academics.dto.response
                                .TimetableGenerateResultResponse.ResultKind.BLOCKED) {
                            run.setStatus(com.thinkerscave.academics.enums.TimetableGenerationRunStatus.COMPLETED_WITH_ISSUES);
                        } else {
                            run.setStatus(com.thinkerscave.academics.enums.TimetableGenerationRunStatus.COMPLETED);
                        }
                        generationRunRepository.save(run);
                    });
        } catch (Exception ex) {
            log.warn("Unable to update generation run for version {}", versionId, ex);
        }
    }
}
