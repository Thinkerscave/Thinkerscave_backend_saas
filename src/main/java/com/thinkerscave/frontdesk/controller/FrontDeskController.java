package com.thinkerscave.frontdesk.controller;

import com.thinkerscave.frontdesk.entity.*;
import com.thinkerscave.frontdesk.service.FrontDeskService;
import com.thinkerscave.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/front-desk")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FrontDeskController {

    private final FrontDeskService service;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Long>>> summary() {
        return ResponseEntity.ok(ApiResponse.success("Front desk summary", service.summary()));
    }

    @GetMapping("/visitors")
    public ResponseEntity<ApiResponse<Page<FrontDeskVisitor>>> visitors(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean activeOnly,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Visitors", service.searchVisitors(keyword, activeOnly, pageable)));
    }

    @PostMapping("/visitors")
    public ResponseEntity<ApiResponse<FrontDeskVisitor>> createVisitor(@RequestBody FrontDeskVisitor body) {
        return ResponseEntity.status(201).body(ApiResponse.created("Visitor checked in", service.createVisitor(body)));
    }

    @PutMapping("/visitors/{id}")
    public ResponseEntity<ApiResponse<FrontDeskVisitor>> updateVisitor(@PathVariable Long id, @RequestBody FrontDeskVisitor body) {
        return ResponseEntity.ok(ApiResponse.success("Visitor updated", service.updateVisitor(id, body)));
    }

    @PostMapping("/visitors/{id}/check-out")
    public ResponseEntity<ApiResponse<FrontDeskVisitor>> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Visitor checked out", service.checkOut(id)));
    }

    @PostMapping(value = "/visitors/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FrontDeskVisitor>> photo(@PathVariable Long id, @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Photo saved", service.storePhoto(id, file)));
    }

    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<Page<FrontDeskAppointment>>> appointments(@RequestParam(required = false) String keyword, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Appointments", service.searchAppointments(keyword, pageable)));
    }

    @PostMapping("/appointments")
    public ResponseEntity<ApiResponse<FrontDeskAppointment>> createAppointment(@RequestBody FrontDeskAppointment body) {
        return ResponseEntity.status(201).body(ApiResponse.created("Appointment saved", service.saveAppointment(null, body)));
    }

    @PutMapping("/appointments/{id}")
    public ResponseEntity<ApiResponse<FrontDeskAppointment>> updateAppointment(@PathVariable Long id, @RequestBody FrontDeskAppointment body) {
        return ResponseEntity.ok(ApiResponse.success("Appointment updated", service.saveAppointment(id, body)));
    }

    @PostMapping("/appointments/{id}/status")
    public ResponseEntity<ApiResponse<FrontDeskAppointment>> appointmentStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime slot) {
        return ResponseEntity.ok(ApiResponse.success("Appointment updated", service.setAppointmentStatus(id, status, slot)));
    }

    @GetMapping("/postal/inward")
    public ResponseEntity<ApiResponse<Page<FrontDeskPostalInward>>> inward(@RequestParam(required = false) String keyword, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Inward mail", service.searchInward(keyword, pageable)));
    }

    @PostMapping("/postal/inward")
    public ResponseEntity<ApiResponse<FrontDeskPostalInward>> createInward(@RequestBody FrontDeskPostalInward body) {
        return ResponseEntity.status(201).body(ApiResponse.created("Inward mail saved", service.saveInward(null, body)));
    }

    @PutMapping("/postal/inward/{id}")
    public ResponseEntity<ApiResponse<FrontDeskPostalInward>> updateInward(@PathVariable Long id, @RequestBody FrontDeskPostalInward body) {
        return ResponseEntity.ok(ApiResponse.success("Inward mail updated", service.saveInward(id, body)));
    }

    @PostMapping("/postal/inward/{id}/hand-over")
    public ResponseEntity<ApiResponse<FrontDeskPostalInward>> handOver(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Marked as handed over", service.handOver(id)));
    }

    @GetMapping("/postal/outward")
    public ResponseEntity<ApiResponse<Page<FrontDeskPostalOutward>>> outward(@RequestParam(required = false) String keyword, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Outward dispatch", service.searchOutward(keyword, pageable)));
    }

    @PostMapping("/postal/outward")
    public ResponseEntity<ApiResponse<FrontDeskPostalOutward>> createOutward(@RequestBody FrontDeskPostalOutward body) {
        return ResponseEntity.status(201).body(ApiResponse.created("Dispatch saved", service.saveOutward(null, body)));
    }

    @PutMapping("/postal/outward/{id}")
    public ResponseEntity<ApiResponse<FrontDeskPostalOutward>> updateOutward(@PathVariable Long id, @RequestBody FrontDeskPostalOutward body) {
        return ResponseEntity.ok(ApiResponse.success("Dispatch updated", service.saveOutward(id, body)));
    }

    @GetMapping("/calls")
    public ResponseEntity<ApiResponse<Page<FrontDeskCallLog>>> calls(@RequestParam(required = false) String keyword, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Call logs", service.searchCalls(keyword, pageable)));
    }

    @PostMapping("/calls")
    public ResponseEntity<ApiResponse<FrontDeskCallLog>> createCall(@RequestBody FrontDeskCallLog body) {
        return ResponseEntity.status(201).body(ApiResponse.created("Call logged", service.saveCall(null, body)));
    }

    @PutMapping("/calls/{id}")
    public ResponseEntity<ApiResponse<FrontDeskCallLog>> updateCall(@PathVariable Long id, @RequestBody FrontDeskCallLog body) {
        return ResponseEntity.ok(ApiResponse.success("Call updated", service.saveCall(id, body)));
    }

    @GetMapping("/grievances")
    public ResponseEntity<ApiResponse<Page<FrontDeskGrievance>>> grievances(@RequestParam(required = false) String keyword, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Grievances", service.searchGrievances(keyword, pageable)));
    }

    @PostMapping("/grievances")
    public ResponseEntity<ApiResponse<FrontDeskGrievance>> createGrievance(@RequestBody FrontDeskGrievance body) {
        return ResponseEntity.status(201).body(ApiResponse.created("Grievance logged", service.saveGrievance(null, body)));
    }

    @PutMapping("/grievances/{id}")
    public ResponseEntity<ApiResponse<FrontDeskGrievance>> updateGrievance(@PathVariable Long id, @RequestBody FrontDeskGrievance body) {
        return ResponseEntity.ok(ApiResponse.success("Grievance updated", service.saveGrievance(id, body)));
    }

    @GetMapping("/certificate-requests")
    public ResponseEntity<ApiResponse<Page<FrontDeskCertificateRequest>>> certificates(@RequestParam(required = false) String keyword, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Certificate requests", service.searchCertificates(keyword, pageable)));
    }

    @PostMapping("/certificate-requests")
    public ResponseEntity<ApiResponse<FrontDeskCertificateRequest>> createCertificate(@RequestBody FrontDeskCertificateRequest body) {
        return ResponseEntity.status(201).body(ApiResponse.created("Certificate request saved", service.createCertificate(body)));
    }

    @PostMapping("/certificate-requests/{id}/approve")
    public ResponseEntity<ApiResponse<FrontDeskCertificateRequest>> approveCertificate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Certificate approved", service.approveCertificate(id)));
    }

    @PostMapping("/certificate-requests/{id}/print")
    public ResponseEntity<ApiResponse<FrontDeskCertificateRequest>> printCertificate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Certificate printed", service.markCertificatePrinted(id)));
    }

    @GetMapping("/gate-passes/attendance-check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> attendance(@RequestParam Long studentId) {
        return ResponseEntity.ok(ApiResponse.success("Attendance status", service.attendanceCheck(studentId)));
    }

    @PostMapping("/gate-passes/otp/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendOtp(@RequestParam Long studentId) {
        return ResponseEntity.ok(ApiResponse.success("OTP sent", service.sendGateOtp(studentId)));
    }

    @PostMapping("/gate-passes/otp/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(@RequestParam Long studentId, @RequestParam String code) {
        return ResponseEntity.ok(ApiResponse.success("OTP verified", service.verifyGateOtp(studentId, code)));
    }

    @GetMapping("/gate-passes")
    public ResponseEntity<ApiResponse<Page<FrontDeskGatePass>>> gatePasses(@RequestParam(required = false) String keyword, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Gate passes", service.searchGatePasses(keyword, pageable)));
    }

    @PostMapping("/gate-passes")
    public ResponseEntity<ApiResponse<FrontDeskGatePass>> issue(@RequestBody FrontDeskGatePass body) {
        return ResponseEntity.status(201).body(ApiResponse.created("Gate pass issued", service.issueGatePass(body)));
    }

    @GetMapping("/directory/students")
    public ResponseEntity<ApiResponse<List<FrontDeskService.StudentDirectoryRow>>> students(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Students", service.studentDirectory(classId, keyword, pageable)));
    }

    @GetMapping("/directory/staff")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> staff(@RequestParam(required = false) String keyword, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Staff", service.staffDirectory(keyword, pageable)));
    }
}
