package com.university.timetable_scheduler.dto.request.timetable;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;

/**
 * One CSV row represents: department → course → lecturer → section → event.
 *
 * Expected CSV header (case-insensitive):
 * courseCode, courseTitle, courseUnit, courseLevel,
 * department,
 * lecturerStaffNumber, lecturerFirstName, lecturerLastName, lecturerEmail,
 * sectionName, sectionEnrollmentSize,
 * eventDurationMinutes, eventType,
 * rooms (optional)    — slash-separated room names e.g. "Classroom1/Classroom2/EDS"
 * timeslot (optional) — slash-separated entries e.g. "M(13:00-15:00)/TH(14:00-17:00)"
 */
@Getter
@Setter
public class BulkUploadTimetableFileRequest {

    // ── Course ────────────────────────────────────────────────────────────
    @CsvBindByName(column = "courseCode", required = true)
    private String courseCode;

    @CsvBindByName(column = "courseTitle", required = true)
    private String courseTitle;

    @CsvBindByName(column = "courseUnit", required = true)
    private Integer courseUnit;

    /** Must match CourseEnum.CourseLevel: LEVEL_100 … LEVEL_500 */
    @CsvBindByName(column = "courseLevel", required = true)
    private String courseLevel;

    // ── Department ────────────────────────────────────────────────────────
    @CsvBindByName(column = "department", required = true)
    private String department;

    // ── Lecturer ──────────────────────────────────────────────────────────
    @CsvBindByName(column = "lecturerStaffNumber", required = true)
    private String lecturerStaffNumber;

    @CsvBindByName(column = "lecturerFirstName", required = true)
    private String lecturerFirstName;

    @CsvBindByName(column = "lecturerLastName", required = true)
    private String lecturerLastName;

    @CsvBindByName(column = "lecturerEmail", required = true)
    private String lecturerEmail;

    // ── Section ───────────────────────────────────────────────────────────
    @CsvBindByName(column = "sectionName", required = true)
    private String sectionName;

    @CsvBindByName(column = "sectionEnrollmentSize", required = true)
    private String sectionEnrollmentSize;


    // ── Event ─────────────────────────────────────────────────────────────
    /** Duration in minutes, e.g. 60 = 1 hour, 120 = 2 hours */
    @CsvBindByName(column = "eventDurationMinutes", required = true)
    private Integer eventDurationMinutes;

    /** Must match EventEnum.EventType: CLASS or LAB */
    @CsvBindByName(column = "eventType", required = true)
    private String eventType;

    // ── Rooms (optional) ─────────────────────────────────────────────────────
    /**
     * Optional. Slash-separated room names.
     * Example: {@code Classroom1/Classroom2/Engineering Drawing Studio (EDS)}
     */
    @CsvBindByName(column = "rooms", required = false)
    private String rooms;

    // ── Timeslot (optional) ───────────────────────────────────────────────────
    /**
     * Optional. Slash-separated timeslot entries in {@code DAY(HH:mm-HH:mm)} format.
     * Supported day abbreviations: M=Monday, T=Tuesday, W=Wednesday,
     *                              TH=Thursday, F=Friday, SAT=Saturday, SUN=Sunday.
     * Example: {@code M(13:00-15:00)/TH(14:00-17:00)}
     */
    @CsvBindByName(column = "timeslot", required = false)
    private String timeslot;
}
