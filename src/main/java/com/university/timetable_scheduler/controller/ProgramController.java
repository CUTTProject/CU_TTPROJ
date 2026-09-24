package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.program.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.program.*;
import com.university.timetable_scheduler.service.impl.ProgramServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/programs")
@AllArgsConstructor
public class ProgramController {
    private final ProgramServiceImpl programService;

    @PostMapping("/create")
    public CreateProgramResponse createProgram(@Valid @RequestBody CreateProgramRequest request) {
        return programService.createProgram(request);
    }

    @Operation(summary = "Paged, filterable list of programmes. 'programName' matches as a "
            + "case-insensitive substring; 'page' defaults to 0 and 'size' to 20 (max 100).")
    @GetMapping("/read")
    public ReadProgramResponse readProgram(@Valid @ModelAttribute ReadProgramRequest request) {
        return programService.readProgram(request);
    }

    @PutMapping("/update")
    public UpdateProgramResponse updateProgram(@Valid @RequestBody UpdateProgramRequest request) {
        return programService.updateProgram(request);
    }

    @DeleteMapping("/delete")
    public DeleteProgramResponse deleteProgram(@Valid @ModelAttribute DeleteProgramRequest request) {
        return programService.deleteProgram(request);
    }

    @Operation(summary = "Headline figures for the programmes page: total, active and inactive "
            + "programmes plus the school's department count.")
    @GetMapping("/stats")
    public ProgramStatsResponse readStats() {
        return programService.readStats();
    }

    @Operation(summary = "Bulk upload programmes from a CSV file. Columns: programCode, programName, "
            + "departmentCode, programCoordinatorStaffNumber, programLevel (UNDERGRADUATE | "
            + "POSTGRADUATE), programDuration, programDescription, programStatus (ACTIVE | INACTIVE). "
            + "Upserts by programCode. Valid rows are saved; rejected rows are listed with the reason. "
            + "Pass dryRun=true to validate and see the counts without saving.")
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BulkUploadResponse bulkUploadPrograms(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return programService.bulkUploadPrograms(file, dryRun);
    }

    @Operation(summary = "Bulk upload programmes from a JSON array. Rows use the CSV column names. Pass "
            + "dryRun=true to validate and see the counts without saving.")
    @PostMapping(value = "/bulk-upload/array", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BulkUploadResponse bulkUploadProgramsArray(
            @Valid @RequestBody BulkUploadProgramArrayRequest request,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return programService.bulkUploadProgramsArray(request, dryRun);
    }
}
