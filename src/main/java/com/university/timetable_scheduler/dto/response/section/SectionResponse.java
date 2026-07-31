package com.university.timetable_scheduler.dto.response.section;

import java.util.UUID;

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
}

