package com.university.timetable_scheduler.dto.response.course;

import java.util.UUID;

import com.university.timetable_scheduler.status.CourseEnum;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseResponse {
    private UUID id;
    private String courseCode;
    private String courseName;
    private String courseDescription;
    private Integer courseUnit;
    private CourseEnum.CourseLevel courseLevel;
    private CourseEnum.CourseStatus courseStatus;
}
