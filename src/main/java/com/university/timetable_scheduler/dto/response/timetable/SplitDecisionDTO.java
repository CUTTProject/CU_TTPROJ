package com.university.timetable_scheduler.dto.response.timetable;

import java.util.List;
import java.util.UUID;

/**
 * How one split=maybe section was planned for a run. Decided afresh on every generation, so it is
 * reported with that run's result rather than stored on the section.
 *
 * @param split         whether this run split the section
 * @param eventMinutes  the durations of the events it was given, longest first
 */
public record SplitDecisionDTO(UUID sectionId, String courseCode, String sectionName,
                               boolean split, List<Long> eventMinutes) {
}
