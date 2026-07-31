package com.university.timetable_scheduler.controller;

import jakarta.validation.Valid;
import com.university.timetable_scheduler.dto.request.timeslot.*;
import com.university.timetable_scheduler.dto.response.timeslot.*;
import com.university.timetable_scheduler.service.impl.TimeslotServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/timeslots")
@AllArgsConstructor
public class TimeslotController {
    private final TimeslotServiceImpl timeslotService;

    @PostMapping("/create")
    public CreateTimeslotResponse createTimeslot(@Valid @RequestBody CreateTimeslotRequest request) {
        return timeslotService.createTimeslot(request);
    }

    @GetMapping("/read")
    public ReadTimeslotResponse readTimeslot(@Valid @ModelAttribute ReadTimeslotRequest request) {
        return timeslotService.readTimeslot(request);
    }

    @PutMapping("/update")
    public UpdateTimeslotResponse updateTimeslot(@Valid @RequestBody UpdateTimeslotRequest request) {
        return timeslotService.updateTimeslot(request);
    }

    @DeleteMapping("/delete")
    public DeleteTimeslotResponse deleteTimeslot(@Valid @ModelAttribute DeleteTimeslotRequest request) {
        return timeslotService.deleteTimeslot(request);
    }
}

