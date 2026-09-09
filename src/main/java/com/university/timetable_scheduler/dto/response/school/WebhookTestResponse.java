package com.university.timetable_scheduler.dto.response.school;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * The outcome of a test delivery: the receiver's status code and nothing else. Relaying the response
 * body would make this a "fetch a URL and show me the result" primitive.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class WebhookTestResponse extends BaseResponse {
    private Data data;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    @Schema(name = "WebhookTestResponseData")
    public static class Data {
        private boolean delivered;

        /** Null if the request never completed. */
        private Integer statusCode;

        private String message;
    }
}
