package com.university.timetable_scheduler.controller;

import jakarta.validation.Valid;
import com.university.timetable_scheduler.dto.request.academicperiod.*;
import com.university.timetable_scheduler.dto.response.academicperiod.*;
import com.university.timetable_scheduler.service.impl.AcademicPeriodServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/academic-periods")
@AllArgsConstructor
public class AcademicPeriodController {
    private final AcademicPeriodServiceImpl academicPeriodService;

    @PostMapping("/create")
    public CreateAcademicPeriodResponse createAcademicPeriod(@Valid @RequestBody CreateAcademicPeriodRequest request) {
        return academicPeriodService.createAcademicPeriod(request);
    }

    @GetMapping("/read")
    public ReadAcademicPeriodResponse readAcademicPeriod(@Valid @ModelAttribute ReadAcademicPeriodRequest request) {
        return academicPeriodService.readAcademicPeriod(request);
    }

    @PutMapping("/update")
    public UpdateAcademicPeriodResponse updateAcademicPeriod(@Valid @RequestBody UpdateAcademicPeriodRequest request) {
        return academicPeriodService.updateAcademicPeriod(request);
    }

    @DeleteMapping("/delete")
    public DeleteAcademicPeriodResponse deleteAcademicPeriod(@Valid @ModelAttribute DeleteAcademicPeriodRequest request) {
        return academicPeriodService.deleteAcademicPeriod(request);
    }
}

