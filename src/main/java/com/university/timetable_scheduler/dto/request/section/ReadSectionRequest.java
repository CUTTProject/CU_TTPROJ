package com.university.timetable_scheduler.dto.request.section;

import java.util.UUID;

import com.university.timetable_scheduler.status.SectionEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadSectionRequest {
    private UUID id;
    private UUID sectionCourseId;
    private String sectionName;
    private UUID sectionAcademicPeriodId;
    private SectionEnum.SectionStatus sectionStatus;
}

