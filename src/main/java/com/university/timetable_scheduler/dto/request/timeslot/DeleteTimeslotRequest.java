package com.university.timetable_scheduler.dto.request.timeslot;

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
public class DeleteTimeslotRequest {
    @NotNull(message = "Timeslot ID is required")
    private UUID id;
}
