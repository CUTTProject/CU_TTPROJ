package com.university.timetable_scheduler.dto.response.timetable;

import java.util.UUID;

/**
 * An event the timetable has no time for, and why.
 *
 * @param courseCode as on the timetable: "PHY 101/PHY 107" for a related pair
 * @param reason     prose for a human
 */
public record UnassignedEventDTO(UUID eventId, String courseCode, String sectionName,
                                 long durationMinutes, String reason) {
}
