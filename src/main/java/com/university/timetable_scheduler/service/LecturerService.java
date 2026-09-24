package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.lecturer.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.lecturer.*;
import org.springframework.web.multipart.MultipartFile;

public interface LecturerService {
    CreateLecturerResponse createLecturer(CreateLecturerRequest request);
    ReadLecturerResponse readLecturer(ReadLecturerRequest request);
    UpdateLecturerResponse updateLecturer(UpdateLecturerRequest request);
    DeleteLecturerResponse deleteLecturer(DeleteLecturerRequest request);

    BulkUploadResponse bulkUploadLecturers(MultipartFile file, boolean dryRun);
    BulkUploadResponse bulkUploadLecturersArray(BulkUploadLecturerArrayRequest request, boolean dryRun);
}
