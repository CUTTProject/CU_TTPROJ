package com.university.timetable_scheduler.dto.request.activity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadActivityRequest {
    /** How many of the newest activities to return; 10 when absent. */
    @Min(1)
    @Max(50)
    private Integer limit;
}
