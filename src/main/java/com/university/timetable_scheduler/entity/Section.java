package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.EventEnum;
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

    /**
     * Total weekly time. The section's events are derived from this and {@link #sectionSplit},
     * at upload and again on every generation. Null on sections whose events were defined one by
     * one before this existed; their events are left as they are.
     */
    private Integer sectionDurationMinutes;

    /**
     * Units for this section, which decide how it splits. The same course can carry different
     * units for different groups of students. Null falls back to the course's units.
     */
    private Integer sectionUnits;

    @Enumerated(EnumType.STRING)
    private SectionEnum.SectionSplit sectionSplit;

    /** A label carried onto the section's events; it does not affect generation. */
    @Enumerated(EnumType.STRING)
    private EventEnum.EventType sectionEventType;

    /**
     * Set on the related section of a {@code relatedTo} pair: it is taught together with this main
     * section, gets no events of its own, and its students and lecturers count against the main
     * section's events. Only pairs: a main section is never itself merged.
     */
    @ManyToOne
    // Not "...IntoId": MySQL Connector/J reads the word "into" in a column list as SELECT ... INTO
    // and rejects every section query with "cannot issue statements that do not produce result sets".
    @JoinColumn(name = "sectionMainSectionId")
    private Section sectionMergedInto;

    /** Whether this is the related section of a live pair, so its main section's events cover it. */
    public boolean isMerged() {
        return sectionMergedInto != null && !Boolean.TRUE.equals(sectionMergedInto.getIsDeleted());
    }

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        sectionStatus = SectionEnum.SectionStatus.ACTIVE;
    }
}