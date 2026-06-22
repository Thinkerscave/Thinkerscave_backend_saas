package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.ChapterRequest;
import com.thinkerscave.academics.dto.request.SyllabusRequest;
import com.thinkerscave.academics.dto.request.TopicProgressRequest;
import com.thinkerscave.academics.dto.request.TopicRequest;
import com.thinkerscave.academics.dto.request.UnitRequest;
import com.thinkerscave.academics.dto.response.ChapterResponse;
import com.thinkerscave.academics.dto.response.SyllabusProgressResponse;
import com.thinkerscave.academics.dto.response.SyllabusResponse;
import com.thinkerscave.academics.dto.response.TopicResponse;
import com.thinkerscave.academics.dto.response.UnitResponse;
import com.thinkerscave.academics.service.SyllabusService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academics/syllabus")
@RequiredArgsConstructor
@Tag(name = "Syllabus", description = "Manage syllabus, units, chapters, topics and progress")
public class SyllabusController {

    private final SyllabusService syllabusService;

    // ---- Syllabus ----

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR')")
    @Operation(summary = "Create syllabus")
    public ResponseEntity<ApiResponse<SyllabusResponse>> create(@Valid @RequestBody SyllabusRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Syllabus created", syllabusService.createSyllabus(request)));
    }

    @GetMapping("/{syllabusId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER','STUDENT','PARENT')")
    @Operation(summary = "Get syllabus by ID with full tree")
    public ResponseEntity<ApiResponse<SyllabusResponse>> getById(@PathVariable Long syllabusId) {
        return ResponseEntity.ok(ApiResponse.success("Syllabus found", syllabusService.getSyllabusById(syllabusId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER','STUDENT','PARENT')")
    @Operation(summary = "Get syllabus by class and year")
    public ResponseEntity<ApiResponse<List<SyllabusResponse>>> getByClassAndYear(
            @RequestParam Long classId, @RequestParam Long yearId) {
        return ResponseEntity.ok(ApiResponse.success("Syllabus list retrieved", syllabusService.getSyllabusByClassAndYear(classId, yearId)));
    }

    @PatchMapping("/{syllabusId}/publish")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR')")
    @Operation(summary = "Publish syllabus")
    public ResponseEntity<ApiResponse<SyllabusResponse>> publish(@PathVariable Long syllabusId) {
        return ResponseEntity.ok(ApiResponse.success("Syllabus published", syllabusService.publishSyllabus(syllabusId)));
    }

    @DeleteMapping("/{syllabusId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Delete syllabus")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long syllabusId) {
        syllabusService.deleteSyllabus(syllabusId);
        return ResponseEntity.ok(ApiResponse.success("Syllabus deleted", null));
    }

    // ---- Unit ----

    @PostMapping("/{syllabusId}/units")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR')")
    @Operation(summary = "Add unit to syllabus")
    public ResponseEntity<ApiResponse<UnitResponse>> addUnit(
            @PathVariable Long syllabusId, @Valid @RequestBody UnitRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Unit added", syllabusService.addUnit(syllabusId, request)));
    }

    @PutMapping("/units/{unitId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR')")
    @Operation(summary = "Update unit")
    public ResponseEntity<ApiResponse<UnitResponse>> updateUnit(
            @PathVariable Long unitId, @Valid @RequestBody UnitRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Unit updated", syllabusService.updateUnit(unitId, request)));
    }

    @GetMapping("/{syllabusId}/units")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER','STUDENT','PARENT')")
    @Operation(summary = "Get units for syllabus")
    public ResponseEntity<ApiResponse<List<UnitResponse>>> getUnits(@PathVariable Long syllabusId) {
        return ResponseEntity.ok(ApiResponse.success("Units retrieved", syllabusService.getUnits(syllabusId)));
    }

    @DeleteMapping("/units/{unitId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Delete unit")
    public ResponseEntity<ApiResponse<Void>> deleteUnit(@PathVariable Long unitId) {
        syllabusService.deleteUnit(unitId);
        return ResponseEntity.ok(ApiResponse.success("Unit deleted", null));
    }

    // ---- Chapter ----

    @PostMapping("/units/{unitId}/chapters")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR')")
    @Operation(summary = "Add chapter to unit")
    public ResponseEntity<ApiResponse<ChapterResponse>> addChapter(
            @PathVariable Long unitId, @Valid @RequestBody ChapterRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Chapter added", syllabusService.addChapter(unitId, request)));
    }

    @PutMapping("/chapters/{chapterId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR')")
    @Operation(summary = "Update chapter")
    public ResponseEntity<ApiResponse<ChapterResponse>> updateChapter(
            @PathVariable Long chapterId, @Valid @RequestBody ChapterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Chapter updated", syllabusService.updateChapter(chapterId, request)));
    }

    @GetMapping("/units/{unitId}/chapters")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER','STUDENT','PARENT')")
    @Operation(summary = "Get chapters for unit")
    public ResponseEntity<ApiResponse<List<ChapterResponse>>> getChapters(@PathVariable Long unitId) {
        return ResponseEntity.ok(ApiResponse.success("Chapters retrieved", syllabusService.getChapters(unitId)));
    }

    @DeleteMapping("/chapters/{chapterId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Delete chapter")
    public ResponseEntity<ApiResponse<Void>> deleteChapter(@PathVariable Long chapterId) {
        syllabusService.deleteChapter(chapterId);
        return ResponseEntity.ok(ApiResponse.success("Chapter deleted", null));
    }

    // ---- Topic ----

    @PostMapping("/chapters/{chapterId}/topics")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR')")
    @Operation(summary = "Add topic to chapter")
    public ResponseEntity<ApiResponse<TopicResponse>> addTopic(
            @PathVariable Long chapterId, @Valid @RequestBody TopicRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Topic added", syllabusService.addTopic(chapterId, request)));
    }

    @PutMapping("/topics/{topicId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR')")
    @Operation(summary = "Update topic")
    public ResponseEntity<ApiResponse<TopicResponse>> updateTopic(
            @PathVariable Long topicId, @Valid @RequestBody TopicRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Topic updated", syllabusService.updateTopic(topicId, request)));
    }

    @GetMapping("/chapters/{chapterId}/topics")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER','STUDENT','PARENT')")
    @Operation(summary = "Get topics for chapter")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> getTopics(@PathVariable Long chapterId) {
        return ResponseEntity.ok(ApiResponse.success("Topics retrieved", syllabusService.getTopics(chapterId)));
    }

    @DeleteMapping("/topics/{topicId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Delete topic")
    public ResponseEntity<ApiResponse<Void>> deleteTopic(@PathVariable Long topicId) {
        syllabusService.deleteTopic(topicId);
        return ResponseEntity.ok(ApiResponse.success("Topic deleted", null));
    }

    // ---- Progress ----

    @PatchMapping("/topics/{topicId}/progress")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Update topic progress (Teacher only)")
    public ResponseEntity<ApiResponse<TopicResponse>> updateProgress(
            @PathVariable Long topicId, @Valid @RequestBody TopicProgressRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Progress updated", syllabusService.updateTopicProgress(topicId, request)));
    }

    @GetMapping("/{syllabusId}/progress")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get syllabus completion progress")
    public ResponseEntity<ApiResponse<SyllabusProgressResponse>> getProgress(
            @PathVariable Long syllabusId,
            @RequestParam(required = false) Long teacherId) {
        return ResponseEntity.ok(ApiResponse.success("Progress retrieved", syllabusService.getSyllabusProgress(syllabusId, teacherId)));
    }
}
