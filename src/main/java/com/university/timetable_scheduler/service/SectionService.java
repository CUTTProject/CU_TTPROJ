package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.section.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.section.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface SectionService {
    CreateSectionResponse createSection(CreateSectionRequest request);
    ReadSectionResponse readSection(ReadSectionRequest request);
    UpdateSectionResponse updateSection(UpdateSectionRequest request);
    DeleteSectionResponse deleteSection(DeleteSectionRequest request);

    BulkUploadResponse bulkUploadSections(MultipartFile file, UUID academicPeriodId, boolean dryRun);
    BulkUploadResponse bulkUploadSectionsArray(BulkUploadSectionArrayRequest request, boolean dryRun);
}
