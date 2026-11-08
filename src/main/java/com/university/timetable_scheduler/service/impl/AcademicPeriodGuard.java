package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.entity.AcademicPeriod;
import com.university.timetable_scheduler.entity.Section;
import com.university.timetable_scheduler.status.AcademicPeriodEnum.AcademicPeriodPhase;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Keeps ended and archived academic periods read-only.
 *
 * <p>Called at every write into a period — its sections, their enrollments, lecturers, timeslots
 * and events, and generation — rather than by a scheduled job that closes periods. The check is
 * exact to the second and works while the host is asleep, which a nightly job on a free instance
 * would not be. Reads and downloads are never checked: an ended period's timetable stays available.
 *
 * <p>The period itself stays editable, since extending its end date or setting it back to ACTIVE
 * is how a school reopens it.
 */
@Component
@AllArgsConstructor
public class AcademicPeriodGuard {

    private static final DateTimeFormatter END_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private final Clock clock;

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public AcademicPeriodPhase phaseOf(AcademicPeriod period) {
        return period.phaseAt(now());
    }

    /** @throws ResponseStatusException 409 when the period has ended or is archived */
    public void requireOpen(AcademicPeriod period) {
        AcademicPeriodPhase phase = phaseOf(period);
        if (phase.isOpen()) {
            return;
        }
        String name = "'" + period.getAcademicPeriodName() + "'";
        String message = phase == AcademicPeriodPhase.ARCHIVED
                ? "Academic period " + name + " is archived, so it can no longer be changed. "
                        + "Set its academicPeriodStatus to ACTIVE to make changes."
                : "Academic period " + name + " ended on " + END_FORMAT.format(period.getAcademicPeriodEndDate())
                        + " (UTC), so it can no longer be changed. Extend its end date to make changes.";
        throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    /** Checks the section's period. A section without one is not tied to any period, so passes. */
    public void requireOpen(Section section) {
        if (section != null && section.getSectionAcademicPeriod() != null) {
            requireOpen(section.getSectionAcademicPeriod());
        }
    }
}
