package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.timetable.BulkUploadTimetableArrayRequest;
import com.university.timetable_scheduler.dto.request.timetable.DownloadConflictGraphRequest;
import com.university.timetable_scheduler.dto.request.timetable.DownloadTimetableRequest;
import com.university.timetable_scheduler.dto.request.timetable.GenerateTimetableRequest;
import com.university.timetable_scheduler.dto.response.timetable.BulkUploadTimetableResponse;
import com.university.timetable_scheduler.dto.response.timetable.TimetableResponse;
import com.university.timetable_scheduler.service.impl.TimetableServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/time-table")
@AllArgsConstructor
public class TimetableController {

    private final TimetableServiceImpl timetableService;

    @Operation(summary = "Bulk upload timetable from CSV. Provide the academicPeriodId of an existing academic period.")
    @PostMapping(value = "bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BulkUploadTimetableResponse bulkUploadTimetable(
            @RequestPart("file") MultipartFile file,
            @RequestParam("academicPeriodId") UUID academicPeriodId) {
        return timetableService.bulkUploadTimetable(file, academicPeriodId);
    }

    @Operation(summary = "Bulk upload timetable from a JSON array. "
            + "academicSession is provided inside the request body.")
    @PostMapping(value = "bulk-upload/array", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BulkUploadTimetableResponse bulkUploadTimetableArray(
            @Valid @RequestBody BulkUploadTimetableArrayRequest request) {
        return timetableService.bulkUploadTimetableArray(request);
    }

    @Operation(summary = "Run the timetable solver. Persist and return the generated schedule.")
    @PostMapping("generate")
    public TimetableResponse generateTimetable(@Valid @ModelAttribute GenerateTimetableRequest generateTimetableRequest ) {
        return timetableService.generateTimetable(generateTimetableRequest);
    }

    @Operation(summary = "Download the persisted timetable as a PDF. ")
    @GetMapping("download/pdf")
    public ResponseEntity<byte[]> downloadTimetablePdf(@Valid @ModelAttribute DownloadTimetableRequest downloadTimetableRequest) {
        byte[] pdf = timetableService.downloadTimetablePdf(downloadTimetableRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"timetable.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Download the section conflict graph as a Graphviz DOT file. "
            + "Open at https://dreampuf.github.io/GraphvizOnline/")
    @GetMapping("conflict-graph/download")
    public ResponseEntity<byte[]> downloadConflictGraph(@Valid @ModelAttribute DownloadConflictGraphRequest downloadConflictGraphRequest) {
        String dot = timetableService.getConflictGraphDot(downloadConflictGraphRequest);
        byte[] bytes = dot.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"conflict-graph.dot\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }
}
