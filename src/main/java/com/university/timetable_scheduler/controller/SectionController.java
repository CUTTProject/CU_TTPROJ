package com.university.timetable_scheduler.controller;

import jakarta.validation.Valid;
import com.university.timetable_scheduler.dto.request.section.*;
import com.university.timetable_scheduler.dto.response.section.*;
import com.university.timetable_scheduler.service.impl.SectionServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sections")
@AllArgsConstructor
public class SectionController {
    private final SectionServiceImpl sectionService;

    @PostMapping("/create")
    public CreateSectionResponse createSection(@Valid @RequestBody CreateSectionRequest request) {
        return sectionService.createSection(request);
    }

    @GetMapping("/read")
    public ReadSectionResponse readSection(@Valid @ModelAttribute ReadSectionRequest request) {
        return sectionService.readSection(request);
    }

    @PutMapping("/update")
    public UpdateSectionResponse updateSection(@Valid @RequestBody UpdateSectionRequest request) {
        return sectionService.updateSection(request);
    }

    @DeleteMapping("/delete")
    public DeleteSectionResponse deleteSection(@Valid @ModelAttribute DeleteSectionRequest request) {
        return sectionService.deleteSection(request);
    }
}

