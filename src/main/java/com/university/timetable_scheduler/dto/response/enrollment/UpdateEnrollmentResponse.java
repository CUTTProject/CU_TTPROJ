package com.university.timetable_scheduler.dto.response.enrollment;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UpdateEnrollmentResponse extends BaseResponse {
    private Data data;

    @Schema(name = "UpdateEnrollmentResponseData")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        private EnrollmentResponse enrollment;
    }
}
