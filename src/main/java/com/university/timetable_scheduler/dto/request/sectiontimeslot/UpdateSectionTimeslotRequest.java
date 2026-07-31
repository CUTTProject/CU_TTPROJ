package com.university.timetable_scheduler.dto.request.sectiontimeslot;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class UpdateSectionTimeslotRequest {
    @NotNull(message = "SectionTimeslot ID is required")
    private UUID id;

    private UUID sectionTimeslotSectionId;
    private UUID sectionTimeslotTimeslotId;
}
