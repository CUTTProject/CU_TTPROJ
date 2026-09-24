package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.lecturer.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.lecturer.*;
import com.university.timetable_scheduler.service.impl.LecturerServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/lecturers")
@AllArgsConstructor
public class LecturerController {
    private final LecturerServiceImpl lecturerService;

    @PostMapping("/create")
    public CreateLecturerResponse createLecturer(@Valid @RequestBody CreateLecturerRequest request) {
        return lecturerService.createLecturer(request);
    }

    @GetMapping("/read")
    public ReadLecturerResponse readLecturer(@Valid @ModelAttribute ReadLecturerRequest request) {
        return lecturerService.readLecturer(request);
    }

    @PutMapping("/update")
    public UpdateLecturerResponse updateLecturer(@Valid @RequestBody UpdateLecturerRequest request) {
        return lecturerService.updateLecturer(request);
    }

    @DeleteMapping("/delete")
    public DeleteLecturerResponse deleteLecturer(@Valid @ModelAttribute DeleteLecturerRequest request) {
        return lecturerService.deleteLecturer(request);
    }

    @Operation(summary = "Bulk upload lecturers from a CSV file. Columns: lecturerStaffNumber, lecturerFirstName, "
            + "lecturerLastName, lecturerEmail, departmentCode. Upserts by lecturerStaffNumber. Valid "
            + "rows are saved; rejected rows are listed with the reason. Pass dryRun=true to validate "
            + "and see the counts without saving.")
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BulkUploadResponse bulkUploadLecturers(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return lecturerService.bulkUploadLecturers(file, dryRun);
    }

    @Operation(summary = "Bulk upload lecturers from a JSON array. Rows use the CSV column names. Pass dryRun=true"
            + " to validate and see the counts without saving.")
    @PostMapping(value = "/bulk-upload/array", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BulkUploadResponse bulkUploadLecturersArray(
            @Valid @RequestBody BulkUploadLecturerArrayRequest request,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return lecturerService.bulkUploadLecturersArray(request, dryRun);
    }
}
