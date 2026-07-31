package com.university.timetable_scheduler.dto.response.timetable;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * A single scheduled event row in the generated timetable.
 */
@Getter
@Setter
public class TimetableEntryDTO {

    private UUID   eventId;
    private String courseCode;
    private String courseName;
    private String sectionName;
    /** "FirstName LastName" of the assigned lecturer (first lecturer if multiple). */
    private String lecturerName;
    private String day;          // e.g. MONDAY
    private String startTime;    // HH:mm
    private String endTime;      // HH:mm
    private String room;
    private long   durationMinutes;
    private String eventType;
}

