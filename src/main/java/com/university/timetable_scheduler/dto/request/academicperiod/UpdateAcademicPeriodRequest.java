package com.university.timetable_scheduler.dto.request.academicperiod;

import com.university.timetable_scheduler.status.AcademicPeriodEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class UpdateAcademicPeriodRequest {
    @NotNull(message = "Academic period ID is required")
    private UUID id;

    private String academicPeriodName;
    private String academicPeriodSession;
    private String academicPeriodSemester;
    private LocalDateTime academicPeriodStartDate;
    private LocalDateTime academicPeriodEndDate;
    /** INACTIVE archives the period (read-only); ACTIVE reopens it. */
    private AcademicPeriodEnum.AcademicPeriodStatus academicPeriodStatus;
}
