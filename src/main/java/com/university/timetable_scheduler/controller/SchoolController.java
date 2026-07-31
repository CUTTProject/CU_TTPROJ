package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.school.*;
import com.university.timetable_scheduler.dto.response.school.*;
import com.university.timetable_scheduler.service.impl.SchoolServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schools")
@AllArgsConstructor
public class SchoolController {

    private final SchoolServiceImpl schoolService;

    @Operation(summary = "Endpoint to register a new school.")
    @PostMapping("/create")
    public CreateSchoolResponse createSchool(@Valid @RequestBody CreateSchoolRequest request) {
        return schoolService.createSchool(request);
    }

    @GetMapping("/read")
    public ReadSchoolResponse readSchool(@Valid @ModelAttribute ReadSchoolRequest request) {
        return schoolService.readSchool(request);
    }

    @PutMapping("/update")
    public UpdateSchoolResponse updateSchool(@Valid @RequestBody UpdateSchoolRequest request) {
        return schoolService.updateSchool(request);
    }

    @DeleteMapping("/delete")
    public DeleteSchoolResponse deleteSchool(@Valid @ModelAttribute DeleteSchoolRequest request) {
        return schoolService.deleteSchool(request);
    }
}

