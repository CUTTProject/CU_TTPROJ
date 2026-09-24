package com.university.timetable_scheduler.dto.request.programcourse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Curriculum bulk upload: which courses each programme takes, at which level and semester.
 * Upserts on {@code programCode + courseCode}.
 *
 * <p>Rows are deliberately not {@code @Valid}-cascaded: BulkUploadSupport validates them one at
 * a time, so a bad row is reported and skipped instead of rejecting the whole request.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BulkUploadProgramCourseArrayRequest {

    @NotEmpty(message = "rows must not be empty")
    private List<Row> rows;

    /** The field names are the CSV column names. */
    @Schema(name = "BulkProgramCourseRow")
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Row {

        @NotBlank(message = "programCode is required")
        private String programCode;

        @NotBlank(message = "courseCode is required")
        private String courseCode;

        /** CourseEnum.CourseLevel: LEVEL_100 ... LEVEL_500, or just 100 ... 500 */
        @NotBlank(message = "level is required")
        private String level;

        /** ProgramCourseEnum.Semester: FIRST | SECOND. Optional. */
        private String semester;

        /** true / false (also yes / no, 1 / 0). Blank means core. */
        private String isCore;
    }
}
