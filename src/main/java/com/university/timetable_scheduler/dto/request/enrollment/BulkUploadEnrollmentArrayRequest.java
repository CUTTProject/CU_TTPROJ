package com.university.timetable_scheduler.dto.request.enrollment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Enrollment bulk upload for one academic period: which student is in which section. Students
 * and sections must already exist. A row matching an existing enrollment is left unchanged.
 *
 * <p>Rows are deliberately not {@code @Valid}-cascaded: BulkUploadSupport validates them one at
 * a time, so a bad row is reported and skipped instead of rejecting the whole request.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BulkUploadEnrollmentArrayRequest {

    @NotNull(message = "academicPeriodId is required")
    private UUID academicPeriodId;

    @NotEmpty(message = "rows must not be empty")
    private List<Row> rows;

    /** The field names are the CSV column names. */
    @Schema(name = "BulkEnrollmentRow")
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Row {

        @NotBlank(message = "studentMatriculationNumber is required")
        private String studentMatriculationNumber;

        @NotBlank(message = "courseCode is required")
        private String courseCode;

        @NotBlank(message = "sectionName is required")
        private String sectionName;
    }
}
