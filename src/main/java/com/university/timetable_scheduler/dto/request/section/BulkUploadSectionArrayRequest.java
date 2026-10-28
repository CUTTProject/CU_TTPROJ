package com.university.timetable_scheduler.dto.request.section;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Section bulk upload for one academic period. A section is identified by
 * {@code courseCode + sectionName} within the period, and has exactly one row.
 *
 * <p>The row is the section's complete definition: re-uploading it replaces its lecturer, room
 * and timeslot links and its events. Events are not uploaded; they are derived from
 * {@code sectionDurationMinutes} and {@code split}. Courses, lecturers and rooms must already
 * exist.
 *
 * <p>Rows are deliberately not {@code @Valid}-cascaded: BulkUploadSupport validates them one at
 * a time, so a bad row is reported and skipped instead of rejecting the whole request.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BulkUploadSectionArrayRequest {

    @NotNull(message = "academicPeriodId is required")
    private UUID academicPeriodId;

    @NotEmpty(message = "rows must not be empty")
    private List<Row> rows;

    /** The field names are the CSV column names. */
    @Schema(name = "BulkSectionRow")
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Row {

        @NotBlank(message = "courseCode is required")
        private String courseCode;

        @NotBlank(message = "sectionName is required")
        private String sectionName;

        @Min(value = 1, message = "sectionEnrollmentSize must be at least 1")
        private Integer sectionEnrollmentSize;

        /** Slash-separated staff numbers, e.g. {@code STF001/STF002}. */
        @NotBlank(message = "lecturerStaffNumbers is required")
        private String lecturerStaffNumbers;

        /** Total weekly time, in minutes. A multiple of 5. */
        @NotNull(message = "sectionDurationMinutes is required")
        @Min(value = 1, message = "sectionDurationMinutes must be at least 1")
        private Integer sectionDurationMinutes;

        /** Optional. This section's units, which decide how it splits; blank uses the course's units. */
        @Min(value = 1, message = "sectionUnits must be at least 1")
        private Integer sectionUnits;

        /** EventEnum.EventType: CLASS | LAB. A label on the events; it does not affect generation. */
        @NotBlank(message = "eventType is required")
        private String eventType;

        /**
         * Optional, default no. yes splits a 3-unit section into 2/3 + 1/3 of the duration and a
         * 4-unit section into halves; other sections cannot split. maybe decides afresh on every
         * generation.
         */
        private String split;

        /**
         * Optional. The sectionName of a section this one is taught together with, e.g.
         * {@code PHY 107-CSC}; it must name one section in the period. This section gets no events
         * of its own: it is taught in that section's events, in a room seating both, and shown as
         * "THIS/RELATED". Pairs only.
         */
        private String relatedTo;

        /**
         * Optional. Slash-separated room numbers the section is restricted to, e.g.
         * {@code LT1/LT2}. Blank means any suitable room.
         */
        private String rooms;

        /**
         * Optional. Slash-separated {@code DAY(HH:mm-HH:mm)} entries, e.g.
         * {@code M(13:00-15:00)/TH(14:00-17:00)}. Missing timeslots are created.
         */
        private String timeslot;
    }
}
