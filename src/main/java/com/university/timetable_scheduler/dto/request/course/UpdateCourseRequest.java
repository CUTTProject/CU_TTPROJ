package com.university.timetable_scheduler.dto.request.course;

import com.university.timetable_scheduler.status.CourseEnum;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.util.UUID;

@Getter
public class UpdateCourseRequest {
    @NotNull(message = "Course ID is required")
    private UUID id;

    @Size(max = 20, message = "Course code must not exceed 20 characters")
    private String courseCode;

    private String courseName;
    private String courseDescription;

    @Min(value = 1, message = "Course unit must be at least 1")
    @Max(value = 10, message = "Course unit must not exceed 10")
    private Integer courseUnit;

    private CourseEnum.CourseLevel courseLevel;
}
