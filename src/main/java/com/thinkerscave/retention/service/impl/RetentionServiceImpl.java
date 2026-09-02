package com.thinkerscave.retention.service.impl;

import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.platform.repository.TenantRegistryRepository;
import com.thinkerscave.retention.RetentionTask;
import com.thinkerscave.retention.RetentionTrigger;
import com.thinkerscave.retention.config.RetentionProperties;
import com.thinkerscave.retention.dto.RetentionPurgeResult;
import com.thinkerscave.retention.dto.RetentionTaskStatus;
import com.thinkerscave.retention.entity.RetentionPurgeLog;
import com.thinkerscave.retention.repository.RetentionPurgeLogRepository;
import com.thinkerscave.retention.service.RetentionService;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionServiceImpl implements RetentionService {

    static final int RECENT_RUNS_LIMIT = 8;

    private static final Pattern IDENTIFIER = Pattern.compile("[a-z_][a-z0-9_]*");

    private final List<RetentionTask> tasks;
    private final RetentionPurgeLogRepository purgeLogRepository;
    private final TenantRegistryRepository tenantRegistryRepository;
    private final JdbcTemplate jdbcTemplate;
    private final RetentionProperties retentionProperties;

    @Override
    @Transactional(readOnly = true)
    public List<RetentionTaskStatus> listTasks() {
        return tasks.stream().map(this::toStatus).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RetentionTaskStatus getTask(String taskKey) {
        return toStatus(requireTask(taskKey));
    }

    @Override
    @Transactional
    public RetentionPurgeResult run(String taskKey, RetentionTrigger trigger, Long organizationId, String actorUsername) {
        RetentionTask task = requireTask(taskKey);
        if (!taskEnabled(task)) {
            throw new BusinessException("Retention task '" + task.key() + "' is disabled");
        }
        int retentionDays = effectiveRetentionDays(task);
        LocalDateTime ranAt = LocalDateTime.now();
        LocalDateTime cutoff = ranAt.minusDays(retentionDays);
        int deleted = purgeTask(task, cutoff, organizationId);
        String actor = StringUtils.hasText(actorUsername) ? actorUsername : "system";
        String summary = buildSummary(task, deleted, cutoff, retentionDays, trigger, organizationId, actor, ranAt);

        RetentionPurgeLog saved = purgeLogRepository.save(RetentionPurgeLog.builder()
                .taskKey(task.key())
                .retentionDays(retentionDays)
                .cutoffAt(cutoff)
                .deletedCount(deleted)
                .triggerType(trigger)
                .organizationId(organizationId)
                .actorUsername(actor)
                .summary(summary)
                .ranAt(ranAt)
                .build());

        log.info(summary);
        return toResult(task, saved);
    }

    private int purgeTask(RetentionTask task, LocalDateTime cutoff, Long organizationId) {
        if (!isSafeIdentifier(task.table()) || !isSafeIdentifier(task.dateColumn())) {
            log.warn("Retention purge skipped for {} — unsafe table/column metadata", task.key());
            return 0;
        }
        int total = 0;
        for (String schema : targetSchemas(organizationId)) {
            if (!isSafeIdentifier(schema) || !tableExists(schema, task.table())) {
                continue;
            }
            try {
                boolean orgScoped = organizationId != null
                        && task.organizationScoped()
                        && StringUtils.hasText(task.organizationClause());
                String sql = orgScoped
                        ? "DELETE FROM " + schema + "." + task.table()
                            + " WHERE " + task.dateColumn() + " < ? AND " + task.organizationClause()
                        : "DELETE FROM " + schema + "." + task.table()
                            + " WHERE " + task.dateColumn() + " < ?";
                Integer deleted = orgScoped
                        ? jdbcTemplate.update(sql, cutoff, organizationId)
                        : jdbcTemplate.update(sql, cutoff);
                total += Math.max(deleted, 0);
            } catch (Exception ex) {
                log.warn("Retention purge failed for {}.{}: {}", schema, task.table(), ex.getMessage());
            }
        }
        return total;
    }

    private List<String> targetSchemas(Long organizationId) {
        Set<String> schemas = new LinkedHashSet<>();
        schemas.add("public");
        if (organizationId != null) {
            tenantRegistryRepository.findByOrganization_Id(organizationId)
                    .map(TenantRegistry::getSchemaName)
                    .filter(StringUtils::hasText)
                    .ifPresent(schemas::add);
            return new ArrayList<>(schemas);
        }
        tenantRegistryRepository.findAll().stream()
                .map(TenantRegistry::getSchemaName)
                .filter(StringUtils::hasText)
                .forEach(schemas::add);
        return new ArrayList<>(schemas);
    }

    private boolean tableExists(String schema, String table) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = ? AND table_name = ?
                """,
                Integer.class,
                schema,
                table);
        return count != null && count > 0;
    }

    private RetentionTask requireTask(String taskKey) {
        Map<String, RetentionTask> byKey = tasks.stream()
                .collect(Collectors.toMap(task -> task.key().toUpperCase(Locale.ROOT), Function.identity(), (a, b) -> a));
        RetentionTask task = byKey.get(taskKey == null ? "" : taskKey.toUpperCase(Locale.ROOT));
        if (task == null) {
            throw new ResourceNotFoundException("Unknown retention task: " + taskKey);
        }
        return task;
    }

    private RetentionTaskStatus toStatus(RetentionTask task) {
        List<RetentionPurgeResult> recent = purgeLogRepository
                .findByTaskKeyOrderByRanAtDesc(task.key(), PageRequest.of(0, RECENT_RUNS_LIMIT))
                .map(logRow -> toResult(task, logRow))
                .getContent();
        RetentionPurgeResult last = recent.isEmpty() ? null : recent.get(0);
        return RetentionTaskStatus.builder()
                .taskKey(task.key())
                .label(task.label())
                .description(task.description())
                .retentionDays(effectiveRetentionDays(task))
                .enabled(retentionProperties.isEnabled() && taskEnabled(task))
                .scheduleCron(retentionProperties.getCron())
                .schedule(scheduleLabel(retentionProperties.getCron()))
                .lastRun(last)
                .recentRuns(recent)
                .nextScheduledHint(retentionProperties.isEnabled() && taskEnabled(task)
                        ? nextDailyOccurrence(retentionProperties.getCron())
                        : null)
                .build();
    }

    private RetentionPurgeResult toResult(RetentionTask task, RetentionPurgeLog logRow) {
        return RetentionPurgeResult.builder()
                .taskKey(task.key())
                .label(task.label())
                .retentionDays(logRow.getRetentionDays())
                .cutoffAt(logRow.getCutoffAt())
                .deletedCount(logRow.getDeletedCount())
                .triggerType(logRow.getTriggerType())
                .organizationId(logRow.getOrganizationId())
                .actorUsername(logRow.getActorUsername())
                .ranAt(logRow.getRanAt())
                .summary(logRow.getSummary())
                .build();
    }

    private String buildSummary(
            RetentionTask task,
            int deleted,
            LocalDateTime cutoff,
            int retentionDays,
            RetentionTrigger trigger,
            Long organizationId,
            String actor,
            LocalDateTime ranAt) {
        String scope = organizationId == null ? "all organizations" : "organization " + organizationId;
        String how = trigger == RetentionTrigger.SCHEDULED ? "automatic nightly job" : "manual run by " + actor;
        return task.label() + ": deleted " + deleted + " row(s) older than "
                + retentionDays + " days (before " + cutoff + ") for " + scope
                + ". " + how + " at " + ranAt + ".";
    }

    private int effectiveRetentionDays(RetentionTask task) {
        RetentionProperties.TaskConfig config = retentionProperties.configOf(task.key());
        if (config != null && config.getRetentionDays() != null && config.getRetentionDays() > 0) {
            return config.getRetentionDays();
        }
        return Math.max(1, task.retentionDays());
    }

    private boolean taskEnabled(RetentionTask task) {
        RetentionProperties.TaskConfig config = retentionProperties.configOf(task.key());
        return config == null || config.getEnabled() == null || config.getEnabled();
    }

    /**
     * Best-effort human label for a simple daily Spring cron ({@code sec min hour * * *}).
     * Anything more complex is echoed back verbatim.
     */
    private String scheduleLabel(String cron) {
        Integer[] parts = parseDailyCron(cron);
        if (parts == null) {
            return StringUtils.hasText(cron) ? "Scheduled (" + cron + ")" : "Not scheduled";
        }
        return String.format("Every day at %02d:%02d", parts[1], parts[2]);
    }

    /**
     * Best-effort next fire time for {@code sec min hour * * *} cron; otherwise tomorrow night.
     */
    private LocalDateTime nextDailyOccurrence(String cron) {
        Integer[] parts = parseDailyCron(cron);
        if (parts != null) {
            LocalTime time = LocalTime.of(parts[1], parts[2], parts[0]);
            LocalDate today = LocalDate.now();
            LocalDateTime candidate = LocalDateTime.of(today, time);
            return candidate.isAfter(LocalDateTime.now()) ? candidate : candidate.plusDays(1);
        }
        return LocalDate.now().plusDays(1).atTime(LocalTime.of(2, 30));
    }

    private Integer[] parseDailyCron(String cron) {
        if (!StringUtils.hasText(cron)) {
            return null;
        }
        String[] fields = cron.trim().split("\\s+");
        if (fields.length != 6) {
            return null;
        }
        if (!"*".equals(fields[3]) || !"*".equals(fields[4]) || !"*".equals(fields[5])) {
            return null;
        }
        try {
            int second = Integer.parseInt(fields[0]);
            int minute = Integer.parseInt(fields[1]);
            int hour = Integer.parseInt(fields[2]);
            if (minute < 0 || minute > 59 || hour < 0 || hour > 23) {
                return null;
            }
            return new Integer[] { second, minute, hour };
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isSafeIdentifier(String value) {
        return StringUtils.hasText(value) && IDENTIFIER.matcher(value).matches();
    }
}