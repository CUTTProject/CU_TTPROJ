package com.university.timetable_scheduler.controller;

import jakarta.validation.Valid;
import com.university.timetable_scheduler.dto.request.lecturer.*;
import com.university.timetable_scheduler.dto.response.lecturer.*;
import com.university.timetable_scheduler.service.impl.LecturerServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lecturers")
@AllArgsConstructor
public class LecturerController {
    private final LecturerServiceImpl lecturerService;

    @PostMapping("/create")
    public CreateLecturerResponse createLecturer(@Valid @RequestBody CreateLecturerRequest request) {
        return lecturerService.createLecturer(request);
    }

    @GetMapping("/read")
    public ReadLecturerResponse readLecturer(@Valid @ModelAttribute ReadLecturerRequest request) {
        return lecturerService.readLecturer(request);
    }

    @PutMapping("/update")
    public UpdateLecturerResponse updateLecturer(@Valid @RequestBody UpdateLecturerRequest request) {
        return lecturerService.updateLecturer(request);
    }

    @DeleteMapping("/delete")
    public DeleteLecturerResponse deleteLecturer(@Valid @ModelAttribute DeleteLecturerRequest request) {
        return lecturerService.deleteLecturer(request);
    }
}

