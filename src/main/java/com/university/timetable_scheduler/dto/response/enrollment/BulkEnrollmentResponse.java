package com.university.timetable_scheduler.dto.response.enrollment;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BulkEnrollmentResponse extends BaseResponse {
    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "BulkEnrollmentResponseData")
    public static class Data {
        private int totalProcessed;
        private int totalCreated;
        private int totalSkipped;
        private List<EnrollmentResponse> enrollments;
        private List<String> skippedReasons;
    }
}

