package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.student.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.student.*;
import org.springframework.web.multipart.MultipartFile;

public interface StudentService {
    CreateStudentResponse createStudent(CreateStudentRequest request);
    ReadStudentResponse readStudent(ReadStudentRequest request);
    UpdateStudentResponse updateStudent(UpdateStudentRequest request);
    DeleteStudentResponse deleteStudent(DeleteStudentRequest request);

    BulkUploadResponse bulkUploadStudents(MultipartFile file, boolean dryRun);
    BulkUploadResponse bulkUploadStudentsArray(BulkUploadStudentArrayRequest request, boolean dryRun);
}
