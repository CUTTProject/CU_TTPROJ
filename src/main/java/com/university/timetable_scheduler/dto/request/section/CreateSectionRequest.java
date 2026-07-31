package com.university.timetable_scheduler.dto.request.section;

import jakarta.validation.constraints.NotBlank;
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
public class CreateSectionRequest {
    @NotNull(message = "Course ID is required")
    private UUID sectionCourseId;

    @NotBlank(message = "Section name is required")
    private String sectionName;

    private String sectionEnrollmentSize;

    @NotNull(message = "Academic period ID is required")
    private UUID sectionAcademicPeriodId;
}
