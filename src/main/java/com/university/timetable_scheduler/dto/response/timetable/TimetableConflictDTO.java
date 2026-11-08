package com.university.timetable_scheduler.dto.response.timetable;

import java.util.UUID;

/**
 * One problem in a generated timetable. For {@code UNASSIGNED} and {@code UNSCHEDULABLE} only
 * {@code eventA} is set, and it has no day, time or room.
 */
public record TimetableConflictDTO(
        UUID conflictId,
        String conflictType,
        String day,
        String reason,
        ConflictEventDTO eventA,
        ConflictEventDTO eventB) {

    public record ConflictEventDTO(
            UUID eventId,
            String courseCode,
            String sectionName,
            String startTime,
            String endTime,
            String room) {
    }
}
