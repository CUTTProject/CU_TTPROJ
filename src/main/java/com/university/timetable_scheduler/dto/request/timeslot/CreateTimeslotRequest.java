package com.university.timetable_scheduler.dto.request.timeslot;

import com.university.timetable_scheduler.status.TimeslotEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateTimeslotRequest {
    @NotNull(message = "Timeslot day is required")
    private TimeslotEnum.TimeslotDay timeslotDay;

    @NotNull(message = "Start time is required")
    private LocalTime timeslotStartTime;

    @NotNull(message = "End time is required")
    private LocalTime timeslotEndTime;

    @NotNull(message = "Duration is required")
    private Duration timeslotDuration;
}
