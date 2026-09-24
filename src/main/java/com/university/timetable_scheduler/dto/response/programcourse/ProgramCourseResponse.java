package com.university.timetable_scheduler.dto.response.programcourse;

import com.university.timetable_scheduler.status.CourseEnum;
import com.university.timetable_scheduler.status.ProgramCourseEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Carries the programme and course codes and names so the curriculum table needs no lookups. */
@Getter
@Setter
public class ProgramCourseResponse {
    private UUID id;
    private UUID programId;
    private String programCode;
    private String programName;
    private UUID courseId;
    private String courseCode;
    private String courseName;
    private CourseEnum.CourseLevel level;
    private ProgramCourseEnum.Semester semester;
    private Boolean isCore;
}
