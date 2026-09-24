package com.university.timetable_scheduler.dto.request.student;

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
 * Student bulk upload. Upserts on {@code studentMatriculationNumber}.
 *
 * <p>A row needs a {@code programCode} or a {@code departmentCode}. With a programme, the
 * student's department is the programme's; the department column is the fallback for schools
 * that have not set up programmes.
 *
 * <p>Rows are deliberately not {@code @Valid}-cascaded: BulkUploadSupport validates them one at
 * a time, so a bad row is reported and skipped instead of rejecting the whole request.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BulkUploadStudentArrayRequest {

    @NotEmpty(message = "rows must not be empty")
    private List<Row> rows;

    /** The field names are the CSV column names. */
    @Schema(name = "BulkStudentRow")
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Row {

        @NotBlank(message = "studentMatriculationNumber is required")
        private String studentMatriculationNumber;

        @NotBlank(message = "studentFirstName is required")
        private String studentFirstName;

        @NotBlank(message = "studentLastName is required")
        private String studentLastName;

        @NotBlank(message = "studentEmail is required")
        @Email(message = "studentEmail must be a valid email address")
        private String studentEmail;

        /** StudentEnum.StudentLevel: LEVEL_100 ... , or just 100 ... */
        @NotBlank(message = "studentLevel is required")
        private String studentLevel;

        private String programCode;

        private String departmentCode;
    }
}
