package com.university.timetable_scheduler.dto.response.sectiontimeslot;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionTimeslotResponse {
    private UUID id;
    private UUID sectionTimeslotSectionId;
    private UUID sectionTimeslotTimeslotId;
}

