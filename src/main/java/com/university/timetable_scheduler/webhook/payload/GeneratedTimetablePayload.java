package com.university.timetable_scheduler.webhook.payload;

import com.university.timetable_scheduler.dto.response.timetable.TimetableEntryDTO;

import java.util.List;
import java.util.UUID;

/**
 * The result the synchronous endpoint used to return inline.
 *
 * <p>One success shape, not two: a run that ran out of budget still arrives here with
 * {@code feasible=false}, and its clashes follow in {@code TIMETABLE_CONFLICTS}.
 *
 * <p>{@code entries} reuses {@link TimetableEntryDTO} — already flat, so no further lookups for a
 * receiver and no JPA proxies on a session-less worker thread.
 */
public record GeneratedTimetablePayload(
        UUID jobId,
        UUID academicPeriodId,
        boolean feasible,
        String stopReason,
        int iterations,
        long elapsedSeconds,
        int totalEvents,
        int scheduledEvents,
        List<TimetableEntryDTO> entries) {
}
