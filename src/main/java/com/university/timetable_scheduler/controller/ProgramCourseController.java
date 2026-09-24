package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.programcourse.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.programcourse.*;
import com.university.timetable_scheduler.service.impl.ProgramCourseServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Programme curricula. Reference data only: timetable generation does not read it. */
@RestController
@RequestMapping("/api/program-courses")
@AllArgsConstructor
public class ProgramCourseController {

    private final ProgramCourseServiceImpl programCourseService;

    @PostMapping("/create")
    public CreateProgramCourseResponse createProgramCourse(@Valid @RequestBody CreateProgramCourseRequest request) {
        return programCourseService.createProgramCourse(request);
    }

    @GetMapping("/read")
    public ReadProgramCourseResponse readProgramCourse(@Valid @ModelAttribute ReadProgramCourseRequest request) {
        return programCourseService.readProgramCourse(request);
    }

    @DeleteMapping("/delete")
    public DeleteProgramCourseResponse deleteProgramCourse(@Valid @ModelAttribute DeleteProgramCourseRequest request) {
        return programCourseService.deleteProgramCourse(request);
    }

    @Operation(summary = "Bulk upload curriculum entries from a CSV file. Columns: programCode, courseCode, level "
            + "(LEVEL_100 ... or 100 ...), semester (FIRST | SECOND, optional), isCore (true | false, "
            + "default true). Upserts by programCode + courseCode. Valid rows are saved; rejected rows "
            + "are listed with the reason. Pass dryRun=true to validate and see the counts without "
            + "saving.")
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BulkUploadResponse bulkUploadProgramCourses(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return programCourseService.bulkUploadProgramCourses(file, dryRun);
    }

    @Operation(summary = "Bulk upload curriculum entries from a JSON array. Rows use the CSV column names. Pass "
            + "dryRun=true to validate and see the counts without saving.")
    @PostMapping(value = "/bulk-upload/array", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BulkUploadResponse bulkUploadProgramCoursesArray(
            @Valid @RequestBody BulkUploadProgramCourseArrayRequest request,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return programCourseService.bulkUploadProgramCoursesArray(request, dryRun);
    }
}
