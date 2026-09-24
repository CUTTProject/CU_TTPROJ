package com.university.timetable_scheduler.dto.request.course;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Course bulk upload. Upserts on {@code courseCode}. {@code courseUnit} and {@code courseLevel}
 * are required to create a course but may be left blank when updating one.
 *
 * <p>Rows are deliberately not {@code @Valid}-cascaded: BulkUploadSupport validates them one at
 * a time, so a bad row is reported and skipped instead of rejecting the whole request.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BulkUploadCourseArrayRequest {

    @NotEmpty(message = "rows must not be empty")
    private List<Row> rows;

    /** The field names are the CSV column names. */
    @Schema(name = "BulkCourseRow")
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Row {

        @NotBlank(message = "courseCode is required")
        @Size(max = 20, message = "courseCode must not exceed 20 characters")
        private String courseCode;

        @NotBlank(message = "courseName is required")
        private String courseName;

        @Min(value = 1, message = "courseUnit must be at least 1")
        @Max(value = 10, message = "courseUnit must not exceed 10")
        private Integer courseUnit;

        /** CourseEnum.CourseLevel: LEVEL_100 ... LEVEL_500, or just 100 ... 500 */
        private String courseLevel;

        @NotBlank(message = "departmentCode is required")
        private String departmentCode;

        private String courseDescription;
    }
}
