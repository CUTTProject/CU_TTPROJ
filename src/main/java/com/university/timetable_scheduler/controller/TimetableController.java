package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.timetable.DownloadConflictGraphRequest;
import com.university.timetable_scheduler.dto.request.timetable.DownloadTimetableRequest;
import com.university.timetable_scheduler.dto.request.timetable.GenerateTimetableRequest;
import com.university.timetable_scheduler.dto.request.timetable.ReadTimetableConflictRequest;
import com.university.timetable_scheduler.dto.response.timetable.ReadTimetableConflictResponse;
import com.university.timetable_scheduler.entity.TimetableGeneration;
import com.university.timetable_scheduler.dto.response.timetable.GenerateTimetableAcceptedResponse;
import com.university.timetable_scheduler.dto.response.timetable.TimetableJobStatusResponse;
import com.university.timetable_scheduler.generation.GenerationJob;
import com.university.timetable_scheduler.generation.GenerationJobRegistry;
import com.university.timetable_scheduler.generation.TimetableGenerationRunner;
import com.university.timetable_scheduler.service.impl.TimetableConflictServiceImpl;
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/time-table")
@AllArgsConstructor
public class TimetableController {

    private final TimetableServiceImpl timetableService;
    private final TimetableGenerationRunner generationRunner;
    private final GenerationJobRegistry jobRegistry;
    private final TimetableConflictServiceImpl conflictService;

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
        GenerationJob job = jobRegistry.find(jobId, TenantContext.getSchoolId()).orElse(null);
        if (job == null) {
            // The in-memory record is gone after a restart, but a finished run was recorded.
            return conflictService.findByJobId(TenantContext.getSchoolId(), jobId)
                    .map(TimetableController::statusFromRecord)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Unknown job. A job that was still running when the server restarted is not "
                                    + "recorded; a timetable that was generated is still persisted and can be "
                                    + "read from GET /api/time-table/download/pdf."));
        }

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
        data.setSplitDecisions(job.getSplitDecisions());

        TimetableJobStatusResponse response = new TimetableJobStatusResponse();
        response.setData(data);
        return response;
    }

    /** A finished run read back from the database. Split decisions are not kept, so they are empty. */
    private static TimetableJobStatusResponse statusFromRecord(TimetableGeneration g) {
        TimetableJobStatusResponse.Data data = new TimetableJobStatusResponse.Data();
        data.setJobId(g.getJobId());
        data.setStatus(g.getGenerationStatus() != null ? g.getGenerationStatus().name() : null);
        data.setAcademicPeriodId(g.getAcademicPeriod() != null ? g.getAcademicPeriod().getId() : null);
        data.setSubmittedAt(g.getSubmittedAt() != null ? g.getSubmittedAt().toString() : null);
        data.setStartedAt(g.getStartedAt() != null ? g.getStartedAt().toString() : null);
        data.setFinishedAt(g.getFinishedAt() != null ? g.getFinishedAt().toString() : null);
        data.setTotalEvents(g.getTotalEvents());
        data.setScheduledEvents(g.getScheduledEvents());
        data.setFeasible(g.getFeasible());
        data.setStopReason(g.getStopReason());
        data.setFailureMessage(g.getFailureMessage());
        data.setSplitDecisions(java.util.List.of());

        TimetableJobStatusResponse response = new TimetableJobStatusResponse();
        response.setData(data);
        return response;
    }

    @Operation(summary = "The conflicts left in the latest generated timetable for an academic period: "
            + "room, lecturer and student clashes, and events that could not be given a time.")
    @GetMapping("conflict")
    public ReadTimetableConflictResponse readConflicts(
            @Valid @ModelAttribute ReadTimetableConflictRequest readTimetableConflictRequest) {
        return conflictService.readLatestConflicts(readTimetableConflictRequest);
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
