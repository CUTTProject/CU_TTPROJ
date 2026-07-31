package com.university.timetable_scheduler.dto.request.timetable;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class DownloadTimetableRequest {
    @NotBlank(message = "Academic period is required")
    private String academicPeriodId;

}
