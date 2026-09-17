package com.university.timetable_scheduler.dto.request.program;

import com.university.timetable_scheduler.status.ProgramEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadProgramRequest {
    private UUID id;

    /** Matched as a case-insensitive substring, so it doubles as the search box. */
    private String programName;

    private UUID programDepartmentId;
    private ProgramEnum.ProgramLevel programLevel;
    private ProgramEnum.ProgramStatus programStatus;

    /** Zero-based page index; 0 when absent. */
    @Min(value = 0, message = "page cannot be negative")
    private Integer page;

    /** Rows per page; 20 when absent. */
    @Min(value = 1, message = "size must be at least 1")
    @Max(value = 100, message = "size cannot exceed 100")
    private Integer size;
}
