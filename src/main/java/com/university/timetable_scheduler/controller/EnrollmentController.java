package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.enrollment.BulkEnrollmentRequest;
import com.university.timetable_scheduler.dto.request.enrollment.CreateEnrollmentRequest;
import com.university.timetable_scheduler.dto.request.enrollment.DeleteEnrollmentRequest;
import com.university.timetable_scheduler.dto.request.enrollment.ReadEnrollmentRequest;
import com.university.timetable_scheduler.dto.request.enrollment.UpdateEnrollmentRequest;
import com.university.timetable_scheduler.dto.response.enrollment.BulkEnrollmentResponse;
import com.university.timetable_scheduler.dto.response.enrollment.CreateEnrollmentResponse;
import com.university.timetable_scheduler.dto.response.enrollment.DeleteEnrollmentResponse;
import com.university.timetable_scheduler.dto.response.enrollment.ReadEnrollmentResponse;
import com.university.timetable_scheduler.dto.response.enrollment.UpdateEnrollmentResponse;
import com.university.timetable_scheduler.service.impl.EnrollmentServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/enrollments")
@AllArgsConstructor
public class EnrollmentController {
    private final EnrollmentServiceImpl enrollmentService;

    @Operation(summary = "Endpoint to add student enrollment")
    @PostMapping("/create")
    public CreateEnrollmentResponse createEnrollment(@Valid @RequestBody CreateEnrollmentRequest request) {
        return enrollmentService.createEnrollment(request);
    }

    @Operation(summary = "Endpoint to read student(s) enrollment data")
    @GetMapping("/read")
    public ReadEnrollmentResponse readEnrollment(@Valid @ModelAttribute ReadEnrollmentRequest request) {
        return enrollmentService.readEnrollment(request);
    }

    @Operation(summary = "Endpoint to update student enrollment data")
    @PutMapping("/update")
    public UpdateEnrollmentResponse updateEnrollment(@Valid @RequestBody UpdateEnrollmentRequest request) {
        return enrollmentService.updateEnrollment(request);
    }

    @Operation(summary = "Endpoint to delete student enrollment data")
    @DeleteMapping("/delete")
    public DeleteEnrollmentResponse deleteEnrollment(@Valid @ModelAttribute DeleteEnrollmentRequest request) {
        return enrollmentService.deleteEnrollment(request);
    }

    @Operation(summary = "Endpoint to bulk create students and enroll them into sections")
    @PostMapping("/bulk")
    public BulkEnrollmentResponse bulkEnroll(@Valid @RequestBody BulkEnrollmentRequest request) {
        return enrollmentService.bulkEnroll(request);
    }

    @Operation(summary = "Endpoint to bulk enroll students from a CSV file. "
            + "Expected columns: studentMatriculationNumber, studentFirstName, studentLastName, "
            + "studentEmail, studentLevel, studentDepartment, courseSection")
    @PostMapping(value = "/bulk/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BulkEnrollmentResponse bulkEnrollFromFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("academicPeriodId") UUID academicPeriodId) {
        return enrollmentService.bulkEnrollFromFile(file, academicPeriodId);
    }
}

