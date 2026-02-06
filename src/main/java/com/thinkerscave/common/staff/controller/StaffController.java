package com.thinkerscave.common.staff.controller;

import com.thinkerscave.common.staff.dto.StaffRequestDTO;
import com.thinkerscave.common.staff.service.StaffService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/staff")
@Tag(name = "Staff Management", description = "Operations related to staff management")
@RequiredArgsConstructor
@Slf4j
public class StaffController {

    private final StaffService staffService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Save or Update Staff", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @PostMapping(value = "/saveOrUpdateStaff", name = "Save or Update Staff Request")
    public ResponseEntity<Map<String, Object>> saveOrUpdateStaffDetails(
            @RequestPart("staffData") StaffRequestDTO staffRequestDTO) {
        log.info("Received request to save/update staff: {} {}", staffRequestDTO.getFirstName(),
                staffRequestDTO.getLastName());
        Map<String, Object> result = staffService.saveOrUpdateStaff(staffRequestDTO);

        if (Boolean.TRUE.equals(result.get("isOutcome"))) {
            return ResponseEntity
                    .ok(Map.of("isOutcome", true, "message", result.get("message"), "data", result.get("data")));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("isOutcome", false, "message", result.get("message")));
        }

    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get all staff members", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @GetMapping("/getAllStaff")
    public ResponseEntity<Map<String, Object>> getAllStaffDetails() {
        log.info("Fetching all staff details");
        Map<String, Object> result = staffService.getAllStaff();

        if (Boolean.TRUE.equals(result.get("isOutcome"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get staff by code", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @GetMapping("/getStaffByCode/{staffCode}")
    public ResponseEntity<Map<String, Object>> getStaffByCode(@PathVariable String staffCode) {
        log.info("Fetching staff by code: {}", staffCode);
        Map<String, Object> result = staffService.getByStaffCode(staffCode);

        if (Boolean.TRUE.equals(result.get("isOutcome"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Set staff active status", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @DeleteMapping("/staffActiveStatus/{staffCode}")
    public ResponseEntity<Map<String, Object>> setStaffActiveStatus(@PathVariable String staffCode) {
        log.info("Changing active status for staff: {}", staffCode);
        Map<String, Object> result = staffService.staffActiveStatus(staffCode);

        if (Boolean.TRUE.equals(result.get("isOutcome"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

}
