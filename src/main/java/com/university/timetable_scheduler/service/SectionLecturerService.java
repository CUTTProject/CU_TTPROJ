package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.sectionlecturer.*;
import com.university.timetable_scheduler.dto.response.sectionlecturer.*;

public interface SectionLecturerService {
    CreateSectionLecturerResponse createSectionLecturer(CreateSectionLecturerRequest request);
    ReadSectionLecturerResponse readSectionLecturer(ReadSectionLecturerRequest request);
    UpdateSectionLecturerResponse updateSectionLecturer(UpdateSectionLecturerRequest request);
    DeleteSectionLecturerResponse deleteSectionLecturer(DeleteSectionLecturerRequest request);
}

