package com.university.timetable_scheduler.dto.response.timeslot;

import java.time.Duration;
import java.time.LocalTime;
import java.util.UUID;

import com.university.timetable_scheduler.status.TimeslotEnum;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimeslotResponse {
    private UUID id;
    private TimeslotEnum.TimeslotDay timeslotDay;
    private LocalTime timeslotStartTime;
    private LocalTime timeslotEndTime;
    private Duration timeslotDuration;
    private TimeslotEnum.TimeslotStatus timeslotStatus;
}

