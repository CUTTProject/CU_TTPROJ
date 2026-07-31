package com.university.timetable_scheduler.dto.request.course;

import java.util.UUID;

import com.university.timetable_scheduler.status.CourseEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ReadCourseRequest {
    private UUID courseId;
    private String courseCode;
    private String courseName;
    private Integer courseUnit;
    private CourseEnum.CourseLevel courseLevel;
    private CourseEnum.CourseStatus courseStatus;
}

