package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.program.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.program.*;
import org.springframework.web.multipart.MultipartFile;

public interface ProgramService {
    CreateProgramResponse createProgram(CreateProgramRequest request);
    ReadProgramResponse readProgram(ReadProgramRequest request);
    UpdateProgramResponse updateProgram(UpdateProgramRequest request);
    DeleteProgramResponse deleteProgram(DeleteProgramRequest request);
    ProgramStatsResponse readStats();
    BulkUploadResponse bulkUploadPrograms(MultipartFile file, boolean dryRun);
    BulkUploadResponse bulkUploadProgramsArray(BulkUploadProgramArrayRequest request, boolean dryRun);
}
