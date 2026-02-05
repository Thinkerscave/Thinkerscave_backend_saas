package com.thinkerscave.common.course.controller;

import com.thinkerscave.common.course.dto.TeacherAllocationDTO;
import com.thinkerscave.common.course.service.TeacherAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 🎓 TeacherAllocationController
 * 
 * Manages the assignment of teachers to specific subjects within a
 * Class/Section context.
 * Critical for generating timetables and workload management.
 */
@CrossOrigin("*")
@RestController
@RequestMapping("/api/v1/allocations")
@Tag(name = "Teacher Allocation", description = "Workload management: Assign teachers to classes and subjects")
@RequiredArgsConstructor
public class TeacherAllocationController {

    private final TeacherAllocationService allocationService;

    @PostMapping
    @Operation(summary = "Assign a teacher", description = "Allocates a teacher to a Subject for a specific Class and Section.")
    public ResponseEntity<TeacherAllocationDTO> allocateTeacher(@RequestBody TeacherAllocationDTO dto) {
        return ResponseEntity.ok(allocationService.allocateTeacher(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update allocation", description = "Modify periods per week or switch teacher for an existing allocation.")
    public ResponseEntity<TeacherAllocationDTO> updateAllocation(
            @PathVariable Long id,
            @RequestBody TeacherAllocationDTO dto) {
        return ResponseEntity.ok(allocationService.updateAllocation(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove allocation", description = "Soft deletes the teacher assignment.")
    public ResponseEntity<Void> deallocateTeacher(@PathVariable Long id) {
        allocationService.deallocateTeacher(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/class/{classId}")
    @Operation(summary = "Get allocations for a class", description = "List all teacher assignments for a specific class and academic year.")
    public ResponseEntity<List<TeacherAllocationDTO>> getByClass(
            @PathVariable Long classId,
            @RequestParam Long academicYearId) {
        return ResponseEntity.ok(allocationService.getAllocationsByClass(classId, academicYearId));
    }

    @GetMapping("/teacher/{teacherId}")
    @Operation(summary = "Get allocations for a teacher", description = "View workload for a specific teacher.")
    public ResponseEntity<List<TeacherAllocationDTO>> getByTeacher(
            @PathVariable Long teacherId,
            @RequestParam Long academicYearId) {
        return ResponseEntity.ok(allocationService.getAllocationsByTeacher(teacherId, academicYearId));
    }
}
