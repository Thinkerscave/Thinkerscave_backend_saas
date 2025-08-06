package com.thinkerscave.common.staff.controller;

import com.thinkerscave.common.staff.dto.StaffRequestDTO;
import com.thinkerscave.common.staff.service.StaffService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/staff")
@Tag(name = "Staff Management", description = "Operations related to staff management")
public class StaffController {

    @Autowired
    private StaffService staffService;

    @PostMapping(value = "/saveOrUpdateStaff", name = "Save or Update Staff Request")
    public ResponseEntity<Map<String, Object>> saveOrUpdateStaffDetails(@RequestPart("staffData") StaffRequestDTO staffRequestDTO) {
        Map<String, Object> result = staffService.saveOrUpdateStaff(staffRequestDTO);

        if (Boolean.TRUE.equals(result.get("isOutcome"))) {
            return ResponseEntity.ok(Map.of("isOutcome", true, "message", result.get("message"), "data", result.get("data")));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("isOutcome", false, "message", result.get("message")));
        }


    }

    @GetMapping("/getAllStaff")
    public ResponseEntity<Map<String, Object>> getAllStaffDetails() {
        Map<String, Object> result = staffService.getAllStaff();

        if (Boolean.TRUE.equals(result.get("isOutcome"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    @GetMapping("/getStaffByCode/{staffCode}")
    public ResponseEntity<Map<String, Object>> getStaffByCode(@PathVariable String staffCode) {
        Map<String, Object> result = staffService.getByStaffCode(staffCode);

        if (Boolean.TRUE.equals(result.get("isOutcome"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    @DeleteMapping("/staffActiveStatus/{staffCode}")
    public ResponseEntity<Map<String, Object>> setStaffActiveStatus(@PathVariable String staffCode) {
        Map<String, Object> result = staffService.staffActiveStatus(staffCode);

        if (Boolean.TRUE.equals(result.get("isOutcome"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

}
