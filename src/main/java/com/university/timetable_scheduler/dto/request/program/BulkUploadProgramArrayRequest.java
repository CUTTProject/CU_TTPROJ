package com.university.timetable_scheduler.dto.request.program;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * JSON body for the array-based programme bulk-upload endpoint.
 * Each row mirrors the fields of the CSV version ({@link BulkUploadProgramFileRequest}).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BulkUploadProgramArrayRequest {

    @NotEmpty(message = "programs must not be empty")
    @Valid
    private List<Row> programs;

    @Schema(name = "BulkProgramRow")
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Row {

        @NotBlank(message = "programName is required")
        private String programName;

        @NotBlank(message = "departmentCode is required")
        private String departmentCode;

        private String programCoordinatorStaffNumber;

        /** Must match ProgramEnum.ProgramLevel: UNDERGRADUATE | POSTGRADUATE */
        private String programLevel;

        @Min(value = 1, message = "programDuration must be at least 1 year")
        private Integer programDuration;

        private String programDescription;

        /** Must match ProgramEnum.ProgramStatus: ACTIVE | INACTIVE */
        private String programStatus;
    }
}
