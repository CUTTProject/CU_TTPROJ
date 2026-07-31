package com.university.timetable_scheduler.dto.request.academicperiod;

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
}
