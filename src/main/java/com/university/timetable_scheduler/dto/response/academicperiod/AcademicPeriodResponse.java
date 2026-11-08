package com.university.timetable_scheduler.dto.response.academicperiod;

import com.university.timetable_scheduler.status.AcademicPeriodEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class AcademicPeriodResponse {
    /** Worked out from the dates and status when the response is built; see AcademicPeriodPhase. */
    private AcademicPeriodEnum.AcademicPeriodPhase academicPeriodPhase;
    private UUID id;
    private String academicPeriodName;
    private String academicPeriodSession;
    private String academicPeriodSemester;
    private LocalDateTime academicPeriodStartDate;
    private LocalDateTime academicPeriodEndDate;
    private AcademicPeriodEnum.AcademicPeriodStatus academicPeriodStatus;
}

