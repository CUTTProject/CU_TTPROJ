package com.university.timetable_scheduler.dto.request.lecturer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Lecturer bulk upload. Upserts on {@code lecturerStaffNumber}.
 *
 * <p>Rows are deliberately not {@code @Valid}-cascaded: BulkUploadSupport validates them one at
 * a time, so a bad row is reported and skipped instead of rejecting the whole request.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BulkUploadLecturerArrayRequest {

    @NotEmpty(message = "rows must not be empty")
    private List<Row> rows;

    /** The field names are the CSV column names. */
    @Schema(name = "BulkLecturerRow")
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Row {

        @NotBlank(message = "lecturerStaffNumber is required")
        private String lecturerStaffNumber;

        @NotBlank(message = "lecturerFirstName is required")
        private String lecturerFirstName;

        @NotBlank(message = "lecturerLastName is required")
        private String lecturerLastName;

        @NotBlank(message = "lecturerEmail is required")
        @Email(message = "lecturerEmail must be a valid email address")
        private String lecturerEmail;

        @NotBlank(message = "departmentCode is required")
        private String departmentCode;
    }
}
