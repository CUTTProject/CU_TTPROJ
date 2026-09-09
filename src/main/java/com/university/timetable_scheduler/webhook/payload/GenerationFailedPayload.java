package com.university.timetable_scheduler.webhook.payload;

import java.util.UUID;

/**
 * {@code reason} is a stable token to branch on; {@code message} is prose that may change.
 * Exception details are omitted — the receiver is outside the trust boundary.
 */
public record GenerationFailedPayload(
        UUID jobId,
        UUID academicPeriodId,
        String reason,
        String message) {

    /** No events, rooms or timeslots to work with — nothing to solve. */
    public static final String NO_MODEL = "NO_SOLVABLE_DATA";

    /** The job threw. */
    public static final String ERROR = "INTERNAL_ERROR";
}
