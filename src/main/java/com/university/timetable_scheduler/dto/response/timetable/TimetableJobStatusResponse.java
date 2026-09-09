package com.university.timetable_scheduler.dto.response.timetable;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

/** The body of {@code GET /api/time-table/generate/{jobId}}. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TimetableJobStatusResponse extends BaseResponse {
    private Data data;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    @Schema(name = "TimetableJobStatusResponseData")
    public static class Data {
        private UUID jobId;
        private String status;
        private UUID academicPeriodId;
        private String submittedAt;
        private String startedAt;
        private String finishedAt;
        private boolean webhookConfigured;

        /** Populated once the run finishes. */
        private Integer totalEvents;
        private Integer scheduledEvents;
        private Boolean feasible;
        private String stopReason;
        private String failureMessage;
    }
}
