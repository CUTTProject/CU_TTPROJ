package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.TimetableEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * One problem left in a generated timetable: a pair of events that clash, or an event that got no
 * time at all ({@code eventB*} empty).
 *
 * <p>The names, times and rooms are copied, not joined: the next generation replaces the events'
 * placements, and duration-based events themselves, so a join would describe a timetable this
 * row never saw. The event ids are plain columns for the same reason.
 */
@Entity
@Table(indexes = @Index(name = "idx_timetable_conflict_generation", columnList = "generationId"))
@NoArgsConstructor
@Getter
@Setter
public class TimetableConflict extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generationId", nullable = false)
    private TimetableGeneration generation;

    @Enumerated(EnumType.STRING)
    private TimetableEnum.TimetableConflictType conflictType;

    /** Day the clash happens on; empty for an event that was never placed. */
    private String conflictDay;

    @Column(length = 1000)
    private String conflictReason;

    private UUID eventAId;
    private String eventACourseCode;
    private String eventASectionName;
    private String eventAStartTime;
    private String eventAEndTime;
    private String eventARoom;

    private UUID eventBId;
    private String eventBCourseCode;
    private String eventBSectionName;
    private String eventBStartTime;
    private String eventBEndTime;
    private String eventBRoom;
}
