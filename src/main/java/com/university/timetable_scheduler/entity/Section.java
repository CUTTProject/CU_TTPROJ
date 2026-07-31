package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.SectionEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Section extends TenantAwareEntity {

    @ManyToOne
    @JoinColumn(name = "sectionCourseId")
    private Course sectionCourse;

    private String sectionName;

    private String sectionEnrollmentSize;

    @ManyToOne
    @JoinColumn(name = "sectionAcademicPeriodId")
    private AcademicPeriod sectionAcademicPeriod;

    @Enumerated(EnumType.STRING)
    private SectionEnum.SectionStatus sectionStatus;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        sectionStatus = SectionEnum.SectionStatus.ACTIVE;
    }
}