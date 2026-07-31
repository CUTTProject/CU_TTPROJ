package com.university.timetable_scheduler.dto.response.event;

import java.time.Duration;
import java.util.UUID;

import com.university.timetable_scheduler.status.EventEnum;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class EventResponse {
    private UUID id;
    private UUID eventSectionId;
    private Duration eventDuration;
    private EventEnum.EventType eventType;
    private EventEnum.EventStatus eventStatus;
}

