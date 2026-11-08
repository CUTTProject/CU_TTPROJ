package com.university.timetable_scheduler.dto.response.timetable;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

/** The body of {@code GET /api/time-table/conflict}. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ReadTimetableConflictResponse extends BaseResponse {
    private Data data;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    @Schema(name = "ReadTimetableConflictResponseData")
    public static class Data {
        /** The latest successful generation for the period. */
        private TimetableGenerationDTO generation;
        /** Clashes first (room, lecturer, students), then events left without a time. */
        private List<TimetableConflictDTO> conflicts;
    }
}
