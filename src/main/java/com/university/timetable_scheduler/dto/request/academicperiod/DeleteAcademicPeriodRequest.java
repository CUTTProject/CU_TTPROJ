package com.university.timetable_scheduler.dto.request.academicperiod;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DeleteAcademicPeriodRequest {
    @NotNull(message = "Academic period ID is required")
    private UUID id;
}
