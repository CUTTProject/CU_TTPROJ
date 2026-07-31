package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.event.*;
import com.university.timetable_scheduler.dto.response.event.*;

public interface EventService {
    CreateEventResponse createEvent(CreateEventRequest request);
    ReadEventResponse readEvent(ReadEventRequest request);
    UpdateEventResponse updateEvent(UpdateEventRequest request);
    DeleteEventResponse deleteEvent(DeleteEventRequest request);
}

