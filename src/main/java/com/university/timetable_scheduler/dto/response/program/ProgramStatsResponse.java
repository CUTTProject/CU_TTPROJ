package com.university.timetable_scheduler.dto.response.program;

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
public class ProgramStatsResponse extends BaseResponse {
    private Data data;

    /** The four cards above the programme table. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ProgramStatsResponseData")
    public static class Data {
        private long totalPrograms;
        private long activePrograms;
        private long inactivePrograms;
        private long departments;
    }
}
