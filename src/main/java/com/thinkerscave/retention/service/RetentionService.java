package com.thinkerscave.retention.service;

import com.thinkerscave.retention.RetentionTrigger;
import com.thinkerscave.retention.dto.RetentionPurgeResult;
import com.thinkerscave.retention.dto.RetentionTaskStatus;

import java.util.List;

public interface RetentionService {

    List<RetentionTaskStatus> listTasks();

    RetentionTaskStatus getTask(String taskKey);

    RetentionPurgeResult run(String taskKey, RetentionTrigger trigger, Long organizationId, String actorUsername);
}
