package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.FixedActivityRequest;
import com.thinkerscave.academics.dto.request.GroupScheduleAssignmentRequest;
import com.thinkerscave.academics.dto.request.SchedulePatternRequest;
import com.thinkerscave.academics.dto.response.FixedActivityResponse;
import com.thinkerscave.academics.dto.response.GroupAssignmentMatrixResponse;
import com.thinkerscave.academics.dto.response.GroupScheduleAssignmentResponse;
import com.thinkerscave.academics.dto.response.SchedulePatternResponse;

import java.util.List;

public interface TimetableConfigurationService {

    List<SchedulePatternResponse> listPatterns(Long yearId);

    SchedulePatternResponse getPattern(Long patternId);

    SchedulePatternResponse createPattern(Long yearId, SchedulePatternRequest request);

    SchedulePatternResponse updatePattern(Long patternId, SchedulePatternRequest request);

    SchedulePatternResponse duplicatePattern(Long patternId, String name);

    SchedulePatternResponse setPatternActive(Long patternId, boolean active);

    List<GroupScheduleAssignmentResponse> listAssignments(Long yearId);

    GroupAssignmentMatrixResponse getAssignmentMatrix(Long yearId);

    List<GroupScheduleAssignmentResponse> assignPattern(Long yearId, GroupScheduleAssignmentRequest request);

    GroupScheduleAssignmentResponse updateAssignment(Long assignmentId, GroupScheduleAssignmentRequest request);

    void deleteAssignment(Long assignmentId);

    List<FixedActivityResponse> listFixedActivities(Long yearId);

    FixedActivityResponse createFixedActivity(Long yearId, FixedActivityRequest request);

    FixedActivityResponse updateFixedActivity(Long activityId, FixedActivityRequest request);

    FixedActivityResponse setFixedActivityActive(Long activityId, boolean active);

    void deleteFixedActivity(Long activityId);
}
