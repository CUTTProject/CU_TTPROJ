package com.university.timetable_scheduler.dto.response.school;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateSchoolResponse extends BaseResponse {
    private Data data;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    @Schema(name = "UpdateSchoolResponseData")
    public static class Data { private SchoolResponse school; }
}
