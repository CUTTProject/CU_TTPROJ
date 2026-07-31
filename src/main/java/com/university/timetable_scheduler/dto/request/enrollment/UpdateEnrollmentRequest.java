package com.university.timetable_scheduler.dto.request.enrollment;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class UpdateEnrollmentRequest {
    @NotNull(message = "Enrollment ID is required")
    private UUID id;

    private UUID enrollmentStudentId;
    private UUID enrollmentSectionId;
    private UUID enrollmentAcademicPeriodId;
}
