package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.ActivityEnum;
import jakarta.persistence.*;
import lombok.AccessLevel;
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

    /**
     * Held as a String, and converted at the accessors, so Hibernate maps an ordinary
     * {@code varchar(255)}.
     *
     * <p>Mapping it as an enum instead — whether through {@code @Enumerated(EnumType.STRING)},
     * {@code @JdbcTypeCode(VARCHAR)} or an {@code AttributeConverter} — makes Hibernate emit a
     * native {@code ENUM(...)} on MySQL and a {@code CHECK (activity_type IN (...))} on
     * PostgreSQL, and {@code ddl-auto=update} never alters an existing column's definition. Every
     * new {@link ActivityEnum.ActivityType} would then need a hand-written migration against each
     * deployed database before the first insert using it could succeed. Hibernate offers no
     * setting to suppress the check constraint, so the enum has to be hidden from the mapping.
     *
     * <p>The column name is unchanged, so this costs no migration of its own.
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String activityType;

    private String activityTitle;

    private String activityDescription;

    /**
     * An Instant rather than the inherited createdAt, which is a zone-less LocalDateTime: the
     * browser renders "2h ago" from this, so it must be an absolute point in time.
     */
    private Instant activityOccurredAt;

    /**
     * Throws on a name this build does not recognise, exactly as the previous
     * {@code @Enumerated(EnumType.STRING)} mapping did: such a row was written by a newer build,
     * and quietly nulling it would strip the field the feed uses to pick an icon.
     */
    public ActivityEnum.ActivityType getActivityType() {
        return activityType == null ? null : ActivityEnum.ActivityType.valueOf(activityType);
    }

    public void setActivityType(ActivityEnum.ActivityType activityType) {
        this.activityType = activityType == null ? null : activityType.name();
    }
}
