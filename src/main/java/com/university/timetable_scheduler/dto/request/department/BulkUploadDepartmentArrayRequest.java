package com.university.timetable_scheduler.dto.request.department;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Department bulk upload. Upserts on {@code departmentCode}.
 *
 * <p>Rows are deliberately not {@code @Valid}-cascaded: BulkUploadSupport validates them one at
 * a time, so a bad row is reported and skipped instead of rejecting the whole request.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BulkUploadDepartmentArrayRequest {

    @NotEmpty(message = "rows must not be empty")
    private List<Row> rows;

    /** The field names are the CSV column names. */
    @Schema(name = "BulkDepartmentRow")
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Row {

        @NotBlank(message = "departmentCode is required")
        @Size(max = 20, message = "departmentCode must not exceed 20 characters")
        private String departmentCode;

        @NotBlank(message = "departmentName is required")
        private String departmentName;

        /**
         * Optional. Lecturers need a department to exist first, so on a first upload this is
         * usually blank; an unknown staff number is a warning, not a rejection.
         */
        private String departmentHeadStaffNumber;
    }
}
