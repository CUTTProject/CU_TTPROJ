package com.university.timetable_scheduler.controller;

import jakarta.validation.Valid;
import com.university.timetable_scheduler.dto.request.sectiontimeslot.*;
import com.university.timetable_scheduler.dto.response.sectiontimeslot.*;
import com.university.timetable_scheduler.service.impl.SectionTimeslotServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/section-timeslots")
@AllArgsConstructor
public class SectionTimeslotController {
    private final SectionTimeslotServiceImpl sectionTimeslotService;

    @PostMapping("/create")
    public CreateSectionTimeslotResponse createSectionTimeslot(@Valid @RequestBody CreateSectionTimeslotRequest request) {
        return sectionTimeslotService.createSectionTimeslot(request);
    }

    @GetMapping("/read")
    public ReadSectionTimeslotResponse readSectionTimeslot(@Valid @ModelAttribute ReadSectionTimeslotRequest request) {
        return sectionTimeslotService.readSectionTimeslot(request);
    }

    @PutMapping("/update")
    public UpdateSectionTimeslotResponse updateSectionTimeslot(@Valid @RequestBody UpdateSectionTimeslotRequest request) {
        return sectionTimeslotService.updateSectionTimeslot(request);
    }

    @DeleteMapping("/delete")
    public DeleteSectionTimeslotResponse deleteSectionTimeslot(@Valid @ModelAttribute DeleteSectionTimeslotRequest request) {
        return sectionTimeslotService.deleteSectionTimeslot(request);
    }
}

