package com.university.timetable_scheduler.controller;

import jakarta.validation.Valid;
import com.university.timetable_scheduler.dto.request.student.*;
import com.university.timetable_scheduler.dto.response.student.*;
import com.university.timetable_scheduler.service.impl.StudentServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@AllArgsConstructor
public class StudentController {
    private final StudentServiceImpl studentService;

    @PostMapping("/create")
    public CreateStudentResponse createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return studentService.createStudent(request);
    }

    @GetMapping("/read")
    public ReadStudentResponse readStudent(@Valid @ModelAttribute ReadStudentRequest request) {
        return studentService.readStudent(request);
    }

    @PutMapping("/update")
    public UpdateStudentResponse updateStudent(@Valid @RequestBody UpdateStudentRequest request) {
        return studentService.updateStudent(request);
    }

    @DeleteMapping("/delete")
    public DeleteStudentResponse deleteStudent(@Valid @ModelAttribute DeleteStudentRequest request) {
        return studentService.deleteStudent(request);
    }
}

