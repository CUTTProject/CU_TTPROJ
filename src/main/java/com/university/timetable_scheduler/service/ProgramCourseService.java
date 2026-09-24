package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.programcourse.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.programcourse.*;
import org.springframework.web.multipart.MultipartFile;

public interface ProgramCourseService {

    CreateProgramCourseResponse createProgramCourse(CreateProgramCourseRequest request);

    ReadProgramCourseResponse readProgramCourse(ReadProgramCourseRequest request);

    DeleteProgramCourseResponse deleteProgramCourse(DeleteProgramCourseRequest request);

    BulkUploadResponse bulkUploadProgramCourses(MultipartFile file, boolean dryRun);

    BulkUploadResponse bulkUploadProgramCoursesArray(BulkUploadProgramCourseArrayRequest request, boolean dryRun);
}
