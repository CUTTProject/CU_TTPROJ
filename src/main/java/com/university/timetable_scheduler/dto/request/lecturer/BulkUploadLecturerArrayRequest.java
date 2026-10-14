package com.university.timetable_scheduler.dto.request.lecturer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import com.university.timetable_scheduler.bulk.Clearable;
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

        /** Optional. A blank cell clears the stored name, a missing column keeps it; without one the lecturer is shown by lecturerStaffNumber. */
        @Clearable
        private String lecturerFirstName;

        @Clearable
        private String lecturerLastName;

        /** Optional, and cleared or kept like the names. Checked only when given. */
        @Clearable
        @Email(message = "lecturerEmail must be a valid email address")
        private String lecturerEmail;

        @NotBlank(message = "departmentCode is required")
        private String departmentCode;
    }
}
