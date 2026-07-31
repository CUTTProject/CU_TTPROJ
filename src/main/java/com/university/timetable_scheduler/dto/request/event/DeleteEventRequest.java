package com.university.timetable_scheduler.dto.request.event;

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
public class DeleteEventRequest {
    @NotNull(message = "Event ID is required")
    private UUID id;
}
