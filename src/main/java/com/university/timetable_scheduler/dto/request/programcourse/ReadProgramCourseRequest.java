package com.university.timetable_scheduler.dto.request.programcourse;

import com.university.timetable_scheduler.status.CourseEnum;
import com.university.timetable_scheduler.status.ProgramCourseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadProgramCourseRequest {
    private UUID programId;
    private UUID courseId;
    private CourseEnum.CourseLevel level;
    private ProgramCourseEnum.Semester semester;
}
