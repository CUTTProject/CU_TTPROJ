package com.university.timetable_scheduler.entity;

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
public class SectionLecturer extends TenantAwareEntity {
    @ManyToOne
    @JoinColumn(name = "sectionLecturerSectionId")
    private Section sectionLecturerSection;

    @ManyToOne
    @JoinColumn(name = "sectionLecturerLecturerId")
    private Lecturer sectionLecturerLecturer;
}
