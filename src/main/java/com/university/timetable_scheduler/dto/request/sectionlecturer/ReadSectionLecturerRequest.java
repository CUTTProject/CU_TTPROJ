package com.university.timetable_scheduler.dto.request.sectionlecturer;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadSectionLecturerRequest {
    private UUID id;
    private UUID sectionLecturerSectionId;
    private UUID sectionLecturerLecturerId;
}

