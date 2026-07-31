package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.TimeslotEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class SectionTimeslot extends TenantAwareEntity {
    @ManyToOne
    @JoinColumn(name = "sectionTimeslotSectionId")
    private Section sectionTimeslotSection;

    @ManyToOne
    @JoinColumn(name = "sectionTimeslotTimeslotId")
    private Timeslot sectionTimeslotTimeslot;
}