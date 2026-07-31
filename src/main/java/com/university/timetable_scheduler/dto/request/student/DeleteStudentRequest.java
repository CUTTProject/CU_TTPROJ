package com.university.timetable_scheduler.dto.request.student;

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
public class DeleteStudentRequest {
    @NotNull(message = "Student ID is required")
    private UUID id;
}
