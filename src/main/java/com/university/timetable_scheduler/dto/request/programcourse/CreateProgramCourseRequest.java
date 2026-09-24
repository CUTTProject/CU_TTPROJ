package com.university.timetable_scheduler.dto.request.programcourse;

import com.university.timetable_scheduler.status.CourseEnum;
import com.university.timetable_scheduler.status.ProgramCourseEnum;
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
public class CreateProgramCourseRequest {
    @NotNull(message = "Program ID is required")
    private UUID programId;

    @NotNull(message = "Course ID is required")
    private UUID courseId;

    @NotNull(message = "Level is required")
    private CourseEnum.CourseLevel level;

    private ProgramCourseEnum.Semester semester;

    /** Core when absent. */
    private Boolean isCore;
}
