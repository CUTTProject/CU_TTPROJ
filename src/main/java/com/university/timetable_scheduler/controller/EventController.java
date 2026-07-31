package com.university.timetable_scheduler.controller;
import jakarta.validation.Valid;
import com.university.timetable_scheduler.dto.request.event.*;
import com.university.timetable_scheduler.dto.response.event.*;
import com.university.timetable_scheduler.service.impl.EventServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@AllArgsConstructor
public class EventController {
    private final EventServiceImpl eventService;

    @PostMapping("/create")
    public CreateEventResponse createEvent(@Valid @RequestBody CreateEventRequest request) {
        return eventService.createEvent(request);
    }

    @GetMapping("/read")
    public ReadEventResponse readEvent(@Valid @ModelAttribute ReadEventRequest request) {
        return eventService.readEvent(request);
    }

    @PutMapping("/update")
    public UpdateEventResponse updateEvent(@Valid @RequestBody UpdateEventRequest request) {
        return eventService.updateEvent(request);
    }

    @DeleteMapping("/delete")
    public DeleteEventResponse deleteEvent(@Valid @ModelAttribute DeleteEventRequest request) {
        return eventService.deleteEvent(request);
    }
}

