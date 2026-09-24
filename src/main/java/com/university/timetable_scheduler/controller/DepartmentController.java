package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.department.BulkUploadDepartmentArrayRequest;
import com.university.timetable_scheduler.dto.request.department.CreateDepartmentRequest;
import com.university.timetable_scheduler.dto.request.department.DeleteDepartmentRequest;
import com.university.timetable_scheduler.dto.request.department.ReadDepartmentRequest;
import com.university.timetable_scheduler.dto.request.department.UpdateDepartmentRequest;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.department.CreateDepartmentResponse;
import com.university.timetable_scheduler.dto.response.department.DeleteDepartmentResponse;
import com.university.timetable_scheduler.dto.response.department.ReadDepartmentResponse;
import com.university.timetable_scheduler.dto.response.department.UpdateDepartmentResponse;
import com.university.timetable_scheduler.service.impl.DepartmentServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/departments")
@AllArgsConstructor
public class DepartmentController {
    private final DepartmentServiceImpl departmentService;

    @PostMapping("/create")
    public CreateDepartmentResponse createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return departmentService.createDepartment(request);
    }

    @GetMapping("/read")
    public ReadDepartmentResponse readDepartment(@Valid @ModelAttribute ReadDepartmentRequest request) {
        return departmentService.readDepartment(request);
    }

    @PutMapping("/update")
    public UpdateDepartmentResponse updateDepartment(@Valid @RequestBody UpdateDepartmentRequest request) {
        return departmentService.updateDepartment(request);
    }

    @DeleteMapping("/delete")
    public DeleteDepartmentResponse deleteDepartment(@Valid @ModelAttribute DeleteDepartmentRequest request) {
        return departmentService.deleteDepartment(request);
    }

    @Operation(summary = "Bulk upload departments from a CSV file. Columns: departmentCode, departmentName, "
            + "departmentHeadStaffNumber (optional). Upserts by departmentCode. Valid rows are saved; "
            + "rejected rows are listed with the reason. Pass dryRun=true to validate and see the "
            + "counts without saving.")
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BulkUploadResponse bulkUploadDepartments(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return departmentService.bulkUploadDepartments(file, dryRun);
    }

    @Operation(summary = "Bulk upload departments from a JSON array. Rows use the CSV column names. Pass "
            + "dryRun=true to validate and see the counts without saving.")
    @PostMapping(value = "/bulk-upload/array", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BulkUploadResponse bulkUploadDepartmentsArray(
            @Valid @RequestBody BulkUploadDepartmentArrayRequest request,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return departmentService.bulkUploadDepartmentsArray(request, dryRun);
    }
}
