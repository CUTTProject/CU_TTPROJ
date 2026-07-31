package com.university.timetable_scheduler.dto.response.school;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ReadSchoolResponse extends BaseResponse {
    private Data data;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    @Schema(name = "ReadSchoolResponseData")
    public static class Data { private List<SchoolResponse> schools; }
}
