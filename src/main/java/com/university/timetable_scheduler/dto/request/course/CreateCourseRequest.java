package com.university.timetable_scheduler.dto.request.course;

import com.university.timetable_scheduler.status.CourseEnum;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class CreateCourseRequest {
    @NotBlank(message = "Course name is required")
    private String courseName;

    @NotBlank(message = "Course code is required")
    @Size(max = 20, message = "Course code must not exceed 20 characters")
    private String courseCode;

    @NotNull(message = "Course unit is required")
    @Min(value = 1, message = "Course unit must be at least 1")
    @Max(value = 10, message = "Course unit must not exceed 10")
    private Integer courseUnit;

    private String courseDescription;

    @NotNull(message = "Course level is required")
    private CourseEnum.CourseLevel courseLevel;
}
