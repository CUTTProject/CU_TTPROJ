package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.section.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.section.*;
import com.university.timetable_scheduler.service.impl.SectionServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/sections")
@AllArgsConstructor
public class SectionController {
    private final SectionServiceImpl sectionService;

    @PostMapping("/create")
    public CreateSectionResponse createSection(@Valid @RequestBody CreateSectionRequest request) {
        return sectionService.createSection(request);
    }

    @GetMapping("/read")
    public ReadSectionResponse readSection(@Valid @ModelAttribute ReadSectionRequest request) {
        return sectionService.readSection(request);
    }

    @PutMapping("/update")
    public UpdateSectionResponse updateSection(@Valid @RequestBody UpdateSectionRequest request) {
        return sectionService.updateSection(request);
    }

    @DeleteMapping("/delete")
    public DeleteSectionResponse deleteSection(@Valid @ModelAttribute DeleteSectionRequest request) {
        return sectionService.deleteSection(request);
    }

    @Operation(summary = "Bulk upload sections and their events for one academic period from a CSV file. Columns: "
            + "courseCode, sectionName, sectionEnrollmentSize, lecturerStaffNumbers (slash-separated), "
            + "eventDurationMinutes, eventType (CLASS | LAB), rooms (slash-separated, optional), "
            + "timeslot (e.g. M(13:00-15:00)/TH(14:00-17:00), optional). One row per event; a section "
            + "is courseCode + sectionName, and re-uploading it replaces its events and links. Valid "
            + "rows are saved; rejected rows are listed with the reason. Pass dryRun=true to validate "
            + "and see the counts without saving.")
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BulkUploadResponse bulkUploadSections(
            @RequestPart("file") MultipartFile file,
            @RequestParam("academicPeriodId") UUID academicPeriodId,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return sectionService.bulkUploadSections(file, academicPeriodId, dryRun);
    }

    @Operation(summary = "Bulk upload sections and their events for one academic period from a JSON array. Rows "
            + "use the CSV column names. Pass dryRun=true to validate and see the counts without "
            + "saving.")
    @PostMapping(value = "/bulk-upload/array", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BulkUploadResponse bulkUploadSectionsArray(
            @Valid @RequestBody BulkUploadSectionArrayRequest request,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return sectionService.bulkUploadSectionsArray(request, dryRun);
    }
}
