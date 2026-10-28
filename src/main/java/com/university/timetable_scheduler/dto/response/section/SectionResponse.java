package com.university.timetable_scheduler.dto.response.section;

import java.util.UUID;

import com.university.timetable_scheduler.status.EventEnum;
import com.university.timetable_scheduler.status.SectionEnum;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionResponse {
    private UUID id;
    private UUID sectionCourseId;
    private String sectionName;
    private String sectionEnrollmentSize;
    private UUID sectionAcademicPeriodId;
    private SectionEnum.SectionStatus sectionStatus;
    private Integer sectionDurationMinutes;
    /** Null means the section uses its course's units. */
    private Integer sectionUnits;
    private SectionEnum.SectionSplit sectionSplit;
    private EventEnum.EventType sectionEventType;
    /** The main section this one is taught with, when it is the related half of a pair. */
    private UUID sectionMergedIntoId;
}

