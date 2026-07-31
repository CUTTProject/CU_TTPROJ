package com.university.timetable_scheduler.dto.request.event;

import com.university.timetable_scheduler.status.EventEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateEventRequest {
    @NotNull(message = "Section ID is required")
    private UUID eventSectionId;

    @NotNull(message = "Event duration is required")
    private Duration eventDuration;

    @NotNull(message = "Event type is required")
    private EventEnum.EventType eventType;
}
