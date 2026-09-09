package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.timetable.BulkUploadTimetableArrayRequest;
import com.university.timetable_scheduler.dto.request.timetable.DownloadConflictGraphRequest;
import com.university.timetable_scheduler.dto.request.timetable.DownloadTimetableRequest;
import com.university.timetable_scheduler.dto.request.timetable.GenerateTimetableRequest;
import com.university.timetable_scheduler.dto.response.timetable.BulkUploadTimetableResponse;
import com.university.timetable_scheduler.dto.response.timetable.GenerateTimetableAcceptedResponse;
import com.university.timetable_scheduler.dto.response.timetable.TimetableJobStatusResponse;
import com.university.timetable_scheduler.generation.GenerationJob;
import com.university.timetable_scheduler.generation.GenerationJobRegistry;
import com.university.timetable_scheduler.generation.TimetableGenerationRunner;
import com.university.timetable_scheduler.service.impl.TimetableServiceImpl;
import com.university.timetable_scheduler.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/time-table")
@AllArgsConstructor
public class TimetableController {

    private final TimetableServiceImpl timetableService;
    private final TimetableGenerationRunner generationRunner;
    private final GenerationJobRegistry jobRegistry;

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

    @Operation(summary = "Queue a timetable generation. Returns a job id immediately; "
            + "the finished timetable is delivered to the school's webhook.")
    @PostMapping("generate")
    public ResponseEntity<GenerateTimetableAcceptedResponse> generateTimetable(
            @Valid @ModelAttribute GenerateTimetableRequest generateTimetableRequest) {

        GenerationJob job = generationRunner.submit(generateTimetableRequest.getAcademicPeriodId());

        GenerateTimetableAcceptedResponse.Data data = new GenerateTimetableAcceptedResponse.Data();
        data.setJobId(job.getJobId());
        data.setStatus(job.getStatus().name());
        data.setAcademicPeriodId(job.getAcademicPeriodId());
        data.setWebhookConfigured(job.isWebhookConfigured());
        data.setStatusUrl("/api/time-table/generate/" + job.getJobId());

        GenerateTimetableAcceptedResponse response = new GenerateTimetableAcceptedResponse();
        response.setData(data);
        response.setResponseCode(HttpStatus.ACCEPTED.toString());
        response.setResponseMessage(job.isWebhookConfigured()
                ? "Generation queued. The timetable will be delivered to your webhook."
                : "Generation queued. No webhook is configured, so poll the status url.");
        return ResponseEntity.accepted().body(response);
    }

    @Operation(summary = "Check on a queued generation.")
    @GetMapping("generate/{jobId}")
    public TimetableJobStatusResponse getGenerationStatus(@PathVariable UUID jobId) {
        GenerationJob job = jobRegistry.find(jobId, TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Unknown job. Job records are held in memory and do not survive a restart; "
                                + "a timetable that was generated is still persisted and can be read "
                                + "from GET /api/time-table/download/pdf."));

        TimetableJobStatusResponse.Data data = new TimetableJobStatusResponse.Data();
        data.setJobId(job.getJobId());
        data.setStatus(job.getStatus().name());
        data.setAcademicPeriodId(job.getAcademicPeriodId());
        data.setSubmittedAt(job.getSubmittedAt() != null ? job.getSubmittedAt().toString() : null);
        data.setStartedAt(job.getStartedAt() != null ? job.getStartedAt().toString() : null);
        data.setFinishedAt(job.getFinishedAt() != null ? job.getFinishedAt().toString() : null);
        data.setWebhookConfigured(job.isWebhookConfigured());
        data.setTotalEvents(job.getTotalEvents());
        data.setScheduledEvents(job.getScheduledEvents());
        data.setFeasible(job.getFeasible());
        data.setStopReason(job.getStopReason());
        data.setFailureMessage(job.getFailureMessage());

        TimetableJobStatusResponse response = new TimetableJobStatusResponse();
        response.setData(data);
        return response;
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
