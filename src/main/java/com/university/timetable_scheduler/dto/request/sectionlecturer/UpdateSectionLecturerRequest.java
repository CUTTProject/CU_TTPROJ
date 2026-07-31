package com.university.timetable_scheduler.dto.request.sectionlecturer;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class UpdateSectionLecturerRequest {
    @NotNull(message = "SectionLecturer ID is required")
    private UUID id;

    private UUID sectionLecturerSectionId;
    private UUID sectionLecturerLecturerId;
}
