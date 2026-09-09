package com.university.timetable_scheduler.webhook.payload;

import java.util.List;
import java.util.UUID;

/**
 * Events whose duration matches no contiguous block of timeslots, so no assignment exists at all —
 * typically a three-hour event where the day is modelled as one-hour slots with no three in a row.
 *
 * <p>A data problem. The solver excludes them from the cost function, which also makes them
 * invisible in it.
 */
public record UnschedulableEventsPayload(
        UUID jobId,
        UUID academicPeriodId,
        int count,
        List<UUID> eventIds) {
}
