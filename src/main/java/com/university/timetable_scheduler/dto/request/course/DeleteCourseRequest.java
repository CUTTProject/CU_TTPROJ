package com.university.timetable_scheduler.dto.request.course;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class DeleteCourseRequest {
    @NotNull(message = "Course ID is required")
    private UUID courseId;
}
