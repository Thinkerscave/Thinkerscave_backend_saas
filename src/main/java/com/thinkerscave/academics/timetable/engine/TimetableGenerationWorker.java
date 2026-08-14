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

    public TimetableGenerationWorker(TimetableGenerationEngine engine,
                                     GenerationProgressTracker tracker) {
        this.engine = engine;
        this.tracker = tracker;
    }

    @Async("timetableGenerationExecutor")
    public void runGeneration(Long versionId, long seed, Long generationId,
                              String tenant, Long organizationId, Long userId) {
        try {
            TenantContext.setTenant(tenant);
            OrganizationContext.setOrganizationId(organizationId);

            log.info("Generation worker started: generationId={}, versionId={}, tenant={}, orgId={}",
                    generationId, versionId, tenant, organizationId);

            GenerationProgress progress = tracker.get(generationId);
            if (progress == null) {
                progress = tracker.init(generationId);
            }

            engine.execute(versionId, seed, progress);

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
}
