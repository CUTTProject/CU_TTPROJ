package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.program.*;
import com.university.timetable_scheduler.dto.response.program.*;
import org.springframework.web.multipart.MultipartFile;

public interface ProgramService {
    CreateProgramResponse createProgram(CreateProgramRequest request);
    ReadProgramResponse readProgram(ReadProgramRequest request);
    UpdateProgramResponse updateProgram(UpdateProgramRequest request);
    DeleteProgramResponse deleteProgram(DeleteProgramRequest request);
    ProgramStatsResponse readStats();
    BulkUploadProgramResponse bulkUploadPrograms(MultipartFile file);
    BulkUploadProgramResponse bulkUploadProgramsArray(BulkUploadProgramArrayRequest request);
}
