package com.university.timetable_scheduler.dto.request.program;

import com.university.timetable_scheduler.status.ProgramEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
