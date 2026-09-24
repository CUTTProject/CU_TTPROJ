package com.university.timetable_scheduler.dto.request.program;

import com.university.timetable_scheduler.status.ProgramEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class UpdateProgramRequest {
    @NotNull(message = "Program ID is required")
    private UUID id;

    private String programName;
    @Size(max = 20, message = "Program code must not exceed 20 characters")
    private String programCode;

    private UUID programDepartmentId;

    private UUID programCoordinatorId;

    private ProgramEnum.ProgramLevel programLevel;

    @Min(value = 1, message = "Program duration must be at least 1 year")
    private Integer programDuration;

    private String programDescription;

    private ProgramEnum.ProgramStatus programStatus;
}
