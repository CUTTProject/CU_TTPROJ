package com.university.timetable_scheduler.dto.request.event;

import java.util.UUID;

import com.university.timetable_scheduler.status.EventEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadEventRequest {
    private UUID id;
    private UUID eventSectionId;
    private EventEnum.EventType eventType;
    private EventEnum.EventStatus eventStatus;
}

