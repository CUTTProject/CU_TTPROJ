package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.student.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.student.*;
import com.university.timetable_scheduler.service.impl.StudentServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/students")
@AllArgsConstructor
public class StudentController {
    private final StudentServiceImpl studentService;

    @PostMapping("/create")
    public CreateStudentResponse createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return studentService.createStudent(request);
    }

    @GetMapping("/read")
    public ReadStudentResponse readStudent(@Valid @ModelAttribute ReadStudentRequest request) {
        return studentService.readStudent(request);
    }

    @PutMapping("/update")
    public UpdateStudentResponse updateStudent(@Valid @RequestBody UpdateStudentRequest request) {
        return studentService.updateStudent(request);
    }

    @DeleteMapping("/delete")
    public DeleteStudentResponse deleteStudent(@Valid @ModelAttribute DeleteStudentRequest request) {
        return studentService.deleteStudent(request);
    }

    @Operation(summary = "Bulk upload students from a CSV file. Columns: studentMatriculationNumber, "
            + "studentFirstName, studentLastName, studentEmail, studentLevel, programCode, "
            + "departmentCode. Upserts by studentMatriculationNumber. Give programCode, or "
            + "departmentCode for a student without a programme. Valid rows are saved; rejected rows "
            + "are listed with the reason. Pass dryRun=true to validate and see the counts without "
            + "saving.")
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BulkUploadResponse bulkUploadStudents(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return studentService.bulkUploadStudents(file, dryRun);
    }

    @Operation(summary = "Bulk upload students from a JSON array. Rows use the CSV column names. Pass dryRun=true "
            + "to validate and see the counts without saving.")
    @PostMapping(value = "/bulk-upload/array", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BulkUploadResponse bulkUploadStudentsArray(
            @Valid @RequestBody BulkUploadStudentArrayRequest request,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return studentService.bulkUploadStudentsArray(request, dryRun);
    }
}
