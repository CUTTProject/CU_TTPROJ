package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.sectiontimeslot.*;
import com.university.timetable_scheduler.dto.response.sectiontimeslot.*;

public interface SectionTimeslotService {
    CreateSectionTimeslotResponse createSectionTimeslot(CreateSectionTimeslotRequest request);
    ReadSectionTimeslotResponse readSectionTimeslot(ReadSectionTimeslotRequest request);
    UpdateSectionTimeslotResponse updateSectionTimeslot(UpdateSectionTimeslotRequest request);
    DeleteSectionTimeslotResponse deleteSectionTimeslot(DeleteSectionTimeslotRequest request);
}

