package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.EventEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Event extends TenantAwareEntity {
    @ManyToOne
    @JoinColumn(name = "eventSectionId")
    private Section eventSection;

    private Duration eventDuration;

    @Enumerated(EnumType.STRING)
    private EventEnum.EventType eventType;

    @Enumerated(EnumType.STRING)
    private EventEnum.EventStatus eventStatus;

    @ManyToOne
    @JoinColumn(name = "eventTimeslotId")
    private Timeslot eventTimeslot;

    @ManyToOne
    @JoinColumn(name = "eventRoomId")
    private Room eventRoom;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        eventStatus = EventEnum.EventStatus.ACTIVE;
    }
}