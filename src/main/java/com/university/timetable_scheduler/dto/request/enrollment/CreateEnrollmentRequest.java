package com.university.timetable_scheduler.dto.request.enrollment;

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
public class CreateEnrollmentRequest {
    @NotNull(message = "Student ID is required")
    private UUID enrollmentStudentId;

    @NotNull(message = "Section ID is required")
    private UUID enrollmentSectionId;

    private UUID enrollmentAcademicPeriodId;
}
