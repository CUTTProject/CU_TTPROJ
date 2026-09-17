package com.university.timetable_scheduler.dto.response.program;

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
public class ReadProgramResponse extends BaseResponse {
    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ReadProgramResponseData")
    public static class Data {
        private List<ProgramResponse> programs;

        /** Zero-based index of the page returned. */
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}
