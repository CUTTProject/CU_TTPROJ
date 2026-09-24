package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.department.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.department.*;
import org.springframework.web.multipart.MultipartFile;

public interface DepartmentService {
    CreateDepartmentResponse createDepartment(CreateDepartmentRequest request);
    ReadDepartmentResponse readDepartment(ReadDepartmentRequest request);
    UpdateDepartmentResponse updateDepartment(UpdateDepartmentRequest request);
    DeleteDepartmentResponse deleteDepartment(DeleteDepartmentRequest request);

    BulkUploadResponse bulkUploadDepartments(MultipartFile file, boolean dryRun);
    BulkUploadResponse bulkUploadDepartmentsArray(BulkUploadDepartmentArrayRequest request, boolean dryRun);
}
