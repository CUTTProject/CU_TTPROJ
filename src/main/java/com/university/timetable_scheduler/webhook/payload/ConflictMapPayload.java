package com.university.timetable_scheduler.webhook.payload;

import java.util.List;
import java.util.UUID;

/**
 * The structural conflict graph over sections: which pairs can never share a timeslot, and why.
 *
 * <p>An explicit edge list rather than the internal adjacency map, which loses the reason per edge
 * and is keyed by a {@code "lowerUuid-higherUuid"} lookup string.
 *
 * <p>When {@code truncated} is true the list is incomplete — do not read it as "all the conflicts".
 */
public record ConflictMapPayload(
        UUID academicPeriodId,
        int sectionCount,
        int edgeCount,
        boolean truncated,
        List<ConflictEdge> conflicts) {

    /** One pair of sections that cannot be scheduled at the same time. */
    public record ConflictEdge(
            UUID sectionA,
            String sectionAName,
            UUID sectionB,
            String sectionBName,
            String reason) {
    }
}
