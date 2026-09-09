package com.university.timetable_scheduler.webhook.payload;

import java.util.List;
import java.util.UUID;

/**
 * Hard-constraint violations left in the generated timetable — {@code ConflictCounter.CostBreakdown}
 * in transport form, which the solver previously computed and then discarded.
 *
 * <p>Conflicts here mean the search ran out of budget; {@code UNSCHEDULABLE_EVENTS} means no budget
 * would have helped.
 */
public record TimetableConflictsPayload(
        UUID jobId,
        UUID academicPeriodId,
        int unassignedEvents,
        int roomClashes,
        int lecturerStudentClashes,
        List<UUID> conflictingEventIds) {
}
