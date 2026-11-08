package com.university.timetable_scheduler.dto.response.timetable;

import java.util.UUID;

/** A finished generation run, as summarised alongside its conflicts. Times are ISO-8601 UTC. */
public record TimetableGenerationDTO(
        UUID generationId,
        UUID jobId,
        UUID academicPeriodId,
        String status,
        String startedAt,
        String finishedAt,
        Boolean feasible,
        String stopReason,
        Integer totalEvents,
        Integer scheduledEvents,
        Integer roomClashes,
        Integer lecturerClashes,
        Integer studentClashes,
        Integer unassignedEvents,
        Integer unschedulableEvents) {
}
