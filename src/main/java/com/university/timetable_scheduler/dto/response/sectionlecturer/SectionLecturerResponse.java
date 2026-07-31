package com.university.timetable_scheduler.dto.response.sectionlecturer;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionLecturerResponse {
    private UUID id;
    private UUID sectionLecturerSectionId;
    private UUID sectionLecturerLecturerId;
}

