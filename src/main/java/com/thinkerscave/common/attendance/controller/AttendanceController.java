package com.thinkerscave.common.attendance.controller;

import com.thinkerscave.common.attendance.domain.Attendance.AttendanceType;
import com.thinkerscave.common.attendance.dto.AttendanceRequestDTO;
import com.thinkerscave.common.attendance.dto.AttendanceResponseDTO;
import com.thinkerscave.common.attendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@Tag(name = "Attendance Management", description = "APIs for managing student, staff, and hostel attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "Save attendance record")
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER','STAFF')")
    public ResponseEntity<AttendanceResponseDTO> save(
            @Valid @RequestBody AttendanceRequestDTO dto,
            Authentication auth) {
        String markedBy = auth != null ? auth.getName() : "SYSTEM";
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.save(dto, markedBy));
    }

    @Operation(summary = "Update an attendance record")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER','STAFF')")
    public ResponseEntity<AttendanceResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceRequestDTO dto) {
        return ResponseEntity.ok(attendanceService.update(id, dto));
    }

    @Operation(summary = "Delete an attendance record")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        attendanceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get class attendance by date and type")
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER','STAFF')")
    public ResponseEntity<List<AttendanceResponseDTO>> getByDateAndType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam AttendanceType type) {
        return ResponseEntity.ok(attendanceService.getByDateAndType(date, type));
    }

    @Operation(summary = "Get class attendance by class ID and date")
    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')")
    public ResponseEntity<List<AttendanceResponseDTO>> getByClass(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getByClassAndDate(classId, date));
    }

    @Operation(summary = "Get attendance history for a specific student/staff/resident")
    @GetMapping("/history/{referenceId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER','STAFF')")
    public ResponseEntity<List<AttendanceResponseDTO>> getHistory(
            @PathVariable Long referenceId,
            @RequestParam AttendanceType type) {
        return ResponseEntity.ok(attendanceService.getByReferenceId(referenceId, type));
    }
}
