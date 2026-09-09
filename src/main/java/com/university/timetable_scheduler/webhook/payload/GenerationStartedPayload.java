package com.university.timetable_scheduler.webhook.payload;

import java.util.UUID;

/**
 * The shape of the problem the solver is about to work on.
 *
 * <p>{@code emptyDomainEvents} above zero means some events can never be placed — knowable now
 * rather than ten minutes from now.
 */
public record GenerationStartedPayload(
        UUID jobId,
        UUID academicPeriodId,
        long timeLimitSeconds,
        int totalEvents,
        int rooms,
        int timeslots,
        double averageDomainSize,
        int emptyDomainEvents) {
}
