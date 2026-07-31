package com.university.timetable_scheduler.dto.request.event;

import com.university.timetable_scheduler.status.EventEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class UpdateEventRequest {
    @NotNull(message = "Event ID is required")
    private UUID id;

    private UUID eventSectionId;
    private Duration eventDuration;
    private EventEnum.EventType eventType;
}
