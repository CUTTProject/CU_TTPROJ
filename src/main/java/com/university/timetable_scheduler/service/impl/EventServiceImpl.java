package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.event.*;
import com.university.timetable_scheduler.dto.response.event.*;
import com.university.timetable_scheduler.entity.Event;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.entity.Section;
import com.university.timetable_scheduler.mapper.EventMapper;
import com.university.timetable_scheduler.repository.EventRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.repository.SectionRepository;
import com.university.timetable_scheduler.service.EventService;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final SectionRepository sectionRepository;
    private final EventMapper eventMapper;
    private final SchoolRepository schoolRepository;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    @Override
    public CreateEventResponse createEvent(CreateEventRequest request) {
        Section section = sectionRepository.findByIdAndSchoolId(request.getEventSectionId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        Event entity = new Event();
        entity.setSchool(currentSchool());
        entity.setEventSection(section);
        entity.setEventDuration(request.getEventDuration());
        entity.setEventType(request.getEventType());
        Event saved = eventRepository.save(entity);
        CreateEventResponse response = new CreateEventResponse();
        CreateEventResponse.Data responseData = new CreateEventResponse.Data();
        responseData.setEvent(eventMapper.toResponse(saved));
        response.setData(responseData);
        return response;
    }

    @Override
    public ReadEventResponse readEvent(ReadEventRequest request) {
        List<Event> list = eventRepository.findEventByFilter(
                TenantContext.getSchoolId(),
                request.getId(), request.getEventSectionId(),
                request.getEventType(), request.getEventStatus());
        ReadEventResponse response = new ReadEventResponse();
        ReadEventResponse.Data responseData = new ReadEventResponse.Data();
        responseData.setEvents(eventMapper.toResponseList(list));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public UpdateEventResponse updateEvent(UpdateEventRequest request) {
        Event entity = eventRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        if (request.getEventSectionId() != null) {
            Section section = sectionRepository.findByIdAndSchoolId(request.getEventSectionId(), TenantContext.getSchoolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
            entity.setEventSection(section);
        }
        if (request.getEventDuration() != null) entity.setEventDuration(request.getEventDuration());
        if (request.getEventType() != null) entity.setEventType(request.getEventType());
        UpdateEventResponse response = new UpdateEventResponse();
        UpdateEventResponse.Data responseData = new UpdateEventResponse.Data();
        responseData.setEvent(eventMapper.toResponse(entity));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public DeleteEventResponse deleteEvent(DeleteEventRequest request) {
        Event entity = eventRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        entity.setIsDeleted(true);
        return new DeleteEventResponse();
    }
}