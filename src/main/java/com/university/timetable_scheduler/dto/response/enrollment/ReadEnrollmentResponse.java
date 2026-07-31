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
public class ReadEnrollmentResponse extends BaseResponse {
    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ReadEnrollmentResponseData")
    public static class Data {
        private List<EnrollmentResponse> enrollments;
    }
}
