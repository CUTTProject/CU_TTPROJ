package com.university.timetable_scheduler.dto.response.school;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/** The webhook settings for a school. Returned by the configure and read endpoints. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class WebhookConfigResponse extends BaseResponse {
    private Data data;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    @Schema(name = "WebhookConfigResponseData")
    public static class Data {
        private String webhookUrl;
        private boolean webhookEnabled;

        /** Whether a secret exists; the value is never readable after it is issued. */
        private boolean secretConfigured;

        /** Present only in the response that created or rotated it; null otherwise. */
        @Schema(accessMode = Schema.AccessMode.READ_ONLY,
                description = "Shown once, when created or rotated. Store it now — it cannot be read back.")
        private String webhookSecret;

        private String note;
    }
}
