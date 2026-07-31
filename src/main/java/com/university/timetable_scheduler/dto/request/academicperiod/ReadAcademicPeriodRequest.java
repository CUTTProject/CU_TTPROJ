package com.university.timetable_scheduler.dto.request.academicperiod;

import com.university.timetable_scheduler.status.AcademicPeriodEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadAcademicPeriodRequest {
    private UUID id;
    private String academicPeriodName;
    private String academicPeriodSession;
    private String academicPeriodSemester;
    private AcademicPeriodEnum.AcademicPeriodStatus academicPeriodStatus;
}

