package com.university.timetable_scheduler.dto.request.program;

import io.swagger.v3.oas.annotations.media.Schema;
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
 * Programme bulk upload. Upserts on {@code programCode}; a programme created before codes
 * existed is matched on name within its department instead, and picks up the code.
 *
 * <p>Rows are deliberately not {@code @Valid}-cascaded: BulkUploadSupport validates them one at
 * a time, so a bad row is reported and skipped instead of rejecting the whole request.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BulkUploadProgramArrayRequest {

    @NotEmpty(message = "programs must not be empty")
    private List<Row> programs;

    /** The field names are the CSV column names. */
    @Schema(name = "BulkProgramRow")
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Row {

        @NotBlank(message = "programCode is required")
        @Size(max = 20, message = "programCode must not exceed 20 characters")
        private String programCode;

        @NotBlank(message = "programName is required")
        private String programName;

        @NotBlank(message = "departmentCode is required")
        private String departmentCode;

        /** Optional. An unknown staff number is a warning; the programme is still saved. */
        private String programCoordinatorStaffNumber;

        /** ProgramEnum.ProgramLevel: UNDERGRADUATE | POSTGRADUATE */
        private String programLevel;

        @Min(value = 1, message = "programDuration must be at least 1 year")
        private Integer programDuration;

        private String programDescription;

        /** ProgramEnum.ProgramStatus: ACTIVE | INACTIVE */
        private String programStatus;
    }
}
