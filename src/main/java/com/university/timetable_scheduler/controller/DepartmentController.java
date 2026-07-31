package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.department.CreateDepartmentRequest;
import com.university.timetable_scheduler.dto.request.department.DeleteDepartmentRequest;
import com.university.timetable_scheduler.dto.request.department.ReadDepartmentRequest;
import com.university.timetable_scheduler.dto.request.department.UpdateDepartmentRequest;
import com.university.timetable_scheduler.dto.response.department.CreateDepartmentResponse;
import com.university.timetable_scheduler.dto.response.department.DeleteDepartmentResponse;
import com.university.timetable_scheduler.dto.response.department.ReadDepartmentResponse;
import com.university.timetable_scheduler.dto.response.department.UpdateDepartmentResponse;
import com.university.timetable_scheduler.service.impl.DepartmentServiceImpl;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/departments")
@AllArgsConstructor
public class DepartmentController {
    private final DepartmentServiceImpl departmentService;

    @PostMapping("/create")
    public CreateDepartmentResponse createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return departmentService.createDepartment(request);
    }

    @GetMapping("/read")
    public ReadDepartmentResponse readDepartment(@Valid @ModelAttribute ReadDepartmentRequest request) {
        return departmentService.readDepartment(request);
    }

    @PutMapping("/update")
    public UpdateDepartmentResponse updateDepartment(@Valid @RequestBody UpdateDepartmentRequest request) {
        return departmentService.updateDepartment(request);
    }

    @DeleteMapping("/delete")
    public DeleteDepartmentResponse deleteDepartment(@Valid @ModelAttribute DeleteDepartmentRequest request) {
        return departmentService.deleteDepartment(request);
    }
}

