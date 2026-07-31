package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.section.*;
import com.university.timetable_scheduler.dto.response.section.*;

public interface SectionService {
    CreateSectionResponse createSection(CreateSectionRequest request);
    ReadSectionResponse readSection(ReadSectionRequest request);
    UpdateSectionResponse updateSection(UpdateSectionRequest request);
    DeleteSectionResponse deleteSection(DeleteSectionRequest request);
}

