package com.university.timetable_scheduler.dto.request.program;

import com.university.timetable_scheduler.status.ProgramEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateProgramRequest {
    @NotBlank(message = "Program name is required")
    private String programName;

    /**
     * Optional here so the existing create form keeps working, but the student and curriculum
     * uploads can only reference a programme that has one. Unique per school.
     */
    @Size(max = 20, message = "Program code must not exceed 20 characters")
    private String programCode;

    @NotNull(message = "Department ID is required")
    private UUID programDepartmentId;

    @NotNull(message = "Program coordinator is required")
    private UUID programCoordinatorId;

    @NotNull(message = "Program level is required")
    private ProgramEnum.ProgramLevel programLevel;

    @NotNull(message = "Program duration is required")
    @Min(value = 1, message = "Program duration must be at least 1 year")
    private Integer programDuration;

    @NotBlank(message = "Description is required")
    private String programDescription;

    /** Optional; a programme is created ACTIVE when this is absent. */
    private ProgramEnum.ProgramStatus programStatus;
}
