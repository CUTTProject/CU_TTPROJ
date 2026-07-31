package com.university.timetable_scheduler.dto.request.sectiontimeslot;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadSectionTimeslotRequest {
    private UUID id;
    private UUID sectionTimeslotSectionId;
    private UUID sectionTimeslotTimeslotId;
}

