package com.university.timetable_scheduler.dto.request.academicperiod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateAcademicPeriodRequest {
    @NotBlank(message = "Academic period name is required")
    private String academicPeriodName;

    @NotBlank(message = "Academic period session is required")
    private String academicPeriodSession;

    @NotBlank(message = "Academic period semester is required")
    private String academicPeriodSemester;

    @NotNull(message = "Academic period start date is required")
    private LocalDateTime academicPeriodStartDate;

    @NotNull(message = "Academic period end date is required")
    private LocalDateTime academicPeriodEndDate;
}
