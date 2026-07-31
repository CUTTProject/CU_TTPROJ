package com.university.timetable_scheduler.dto.request.section;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class UpdateSectionRequest {
    @NotNull(message = "Section ID is required")
    private UUID id;

    private UUID sectionCourseId;
    private String sectionName;
    private String sectionEnrollmentSize;
    private UUID sectionAcademicPeriodId;
}
