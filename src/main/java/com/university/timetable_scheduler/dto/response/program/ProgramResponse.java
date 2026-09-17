package com.university.timetable_scheduler.dto.response.program;

import com.university.timetable_scheduler.status.ProgramEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Carries the department and coordinator names alongside their ids: the programme table
 * renders both, and returning ids alone would force the client into a lookup per row.
 */
@Getter
@Setter
public class ProgramResponse {
    private UUID id;
    private String programName;
    private UUID programDepartmentId;
    private String programDepartmentName;
    private UUID programCoordinatorId;
    private String programCoordinatorName;
    private ProgramEnum.ProgramLevel programLevel;
    private Integer programDuration;
    private String programDescription;
    private ProgramEnum.ProgramStatus programStatus;

    /** Live students currently attached to this programme. Derived, never stored. */
    private long studentCount;
}
