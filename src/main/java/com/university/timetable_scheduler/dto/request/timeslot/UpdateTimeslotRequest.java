package com.university.timetable_scheduler.dto.request.timeslot;

import com.university.timetable_scheduler.status.TimeslotEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalTime;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class UpdateTimeslotRequest {
    @NotNull(message = "Timeslot ID is required")
    private UUID id;

    private TimeslotEnum.TimeslotDay timeslotDay;
    private LocalTime timeslotStartTime;
    private LocalTime timeslotEndTime;
    private Duration timeslotDuration;
}
