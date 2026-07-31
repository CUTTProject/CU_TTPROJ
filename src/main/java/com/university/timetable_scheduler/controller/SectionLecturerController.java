package com.university.timetable_scheduler.controller;

import jakarta.validation.Valid;
import com.university.timetable_scheduler.dto.request.sectionlecturer.*;
import com.university.timetable_scheduler.dto.response.sectionlecturer.*;
import com.university.timetable_scheduler.service.impl.SectionLecturerServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/section-lecturers")
@AllArgsConstructor
public class SectionLecturerController {
    private final SectionLecturerServiceImpl sectionLecturerService;

    @PostMapping("/create")
    public CreateSectionLecturerResponse createSectionLecturer(@Valid @RequestBody CreateSectionLecturerRequest request) {
        return sectionLecturerService.createSectionLecturer(request);
    }

    @GetMapping("/read")
    public ReadSectionLecturerResponse readSectionLecturer(@Valid @ModelAttribute ReadSectionLecturerRequest request) {
        return sectionLecturerService.readSectionLecturer(request);
    }

    @PutMapping("/update")
    public UpdateSectionLecturerResponse updateSectionLecturer(@Valid @RequestBody UpdateSectionLecturerRequest request) {
        return sectionLecturerService.updateSectionLecturer(request);
    }

    @DeleteMapping("/delete")
    public DeleteSectionLecturerResponse deleteSectionLecturer(@Valid @ModelAttribute DeleteSectionLecturerRequest request) {
        return sectionLecturerService.deleteSectionLecturer(request);
    }
}

