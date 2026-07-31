package com.university.timetable_scheduler.dto.request.sectiontimeslot;

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
public class CreateSectionTimeslotRequest {
    @NotNull(message = "Section ID is required")
    private UUID sectionTimeslotSectionId;

    @NotNull(message = "Timeslot ID is required")
    private UUID sectionTimeslotTimeslotId;
}
