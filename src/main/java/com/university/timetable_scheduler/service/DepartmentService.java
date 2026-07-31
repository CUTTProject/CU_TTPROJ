package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.department.*;
import com.university.timetable_scheduler.dto.response.department.*;

public interface DepartmentService {
    CreateDepartmentResponse createDepartment(CreateDepartmentRequest request);
    ReadDepartmentResponse readDepartment(ReadDepartmentRequest request);
    UpdateDepartmentResponse updateDepartment(UpdateDepartmentRequest request);
    DeleteDepartmentResponse deleteDepartment(DeleteDepartmentRequest request);
}

