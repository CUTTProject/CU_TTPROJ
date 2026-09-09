package com.university.timetable_scheduler.dto.response.timetable;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

/**
 * The 202 body from {@code POST /api/time-table/generate}. A run lasts longer than any proxy holds a
 * request open, so the caller gets a job id and the timetable arrives at the school's webhook.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class GenerateTimetableAcceptedResponse extends BaseResponse {
    private Data data;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    @Schema(name = "GenerateTimetableAcceptedResponseData")
    public static class Data {
        private UUID jobId;
        private String status;
        private UUID academicPeriodId;

        /**
         * False when no webhook is configured or it is disabled. The run still happens and still
         * persists; the caller just has to poll and read the timetable itself.
         */
        private boolean webhookConfigured;

        private String statusUrl;
    }
}
