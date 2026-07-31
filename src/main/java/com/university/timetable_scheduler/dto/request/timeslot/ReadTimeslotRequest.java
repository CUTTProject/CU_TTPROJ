package com.university.timetable_scheduler.dto.request.timeslot;

import java.util.UUID;

import com.university.timetable_scheduler.status.TimeslotEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadTimeslotRequest {
    private UUID id;
    private TimeslotEnum.TimeslotDay timeslotDay;
    private TimeslotEnum.TimeslotStatus timeslotStatus;
}

