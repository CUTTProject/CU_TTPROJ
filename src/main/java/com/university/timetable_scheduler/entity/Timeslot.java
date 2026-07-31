package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.TimeslotEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalTime;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Timeslot extends TenantAwareEntity {

    @Enumerated(EnumType.STRING)
    private TimeslotEnum.TimeslotDay timeslotDay;

    private LocalTime timeslotStartTime;

    private LocalTime timeslotEndTime;

    private Duration timeslotDuration;

    @Enumerated(EnumType.STRING)
    private TimeslotEnum.TimeslotStatus timeslotStatus;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        timeslotStatus = TimeslotEnum.TimeslotStatus.ACTIVE;
    }
}