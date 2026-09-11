package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.ActivityEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One line of a school's "Recent Activities" feed. Written by the services at the point the thing
 * happened; never updated.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(indexes = @Index(name = "idx_activity_school_occurred_at", columnList = "schoolId, activityOccurredAt"))
public class Activity extends TenantAwareEntity {

    @Enumerated(EnumType.STRING)
    private ActivityEnum.ActivityType activityType;

    private String activityTitle;

    private String activityDescription;

    /**
     * An Instant rather than the inherited createdAt, which is a zone-less LocalDateTime: the
     * browser renders "2h ago" from this, so it must be an absolute point in time.
     */
    private Instant activityOccurredAt;
}
