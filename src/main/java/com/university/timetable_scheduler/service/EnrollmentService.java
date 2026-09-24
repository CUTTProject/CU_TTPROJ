package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.enrollment.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.enrollment.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface EnrollmentService {
    CreateEnrollmentResponse createEnrollment(CreateEnrollmentRequest request);
    ReadEnrollmentResponse readEnrollment(ReadEnrollmentRequest request);
    UpdateEnrollmentResponse updateEnrollment(UpdateEnrollmentRequest request);
    DeleteEnrollmentResponse deleteEnrollment(DeleteEnrollmentRequest request);

    BulkUploadResponse bulkUploadEnrollments(MultipartFile file, UUID academicPeriodId, boolean dryRun);
    BulkUploadResponse bulkUploadEnrollmentsArray(BulkUploadEnrollmentArrayRequest request, boolean dryRun);
}
