package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.response.event.EventResponse;
import com.university.timetable_scheduler.entity.Event;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EventMapper {
    @Mapping(source = "eventSection.id", target = "eventSectionId")
    EventResponse toResponse(Event event);

    List<EventResponse> toResponseList(List<Event> events);
}

