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
 * Section and event bulk upload for one academic period. A section is identified by
 * {@code courseCode + sectionName} within the period.
 *
 * <p>Each row is one event. The rows for a section are its complete definition: re-uploading
 * a section replaces its events and its lecturer, room and timeslot links, rather than adding to
 * them. Courses, lecturers and rooms must already exist.
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

        @NotNull(message = "eventDurationMinutes is required")
        @Min(value = 1, message = "eventDurationMinutes must be at least 1")
        private Integer eventDurationMinutes;

        /** EventEnum.EventType: CLASS | LAB */
        @NotBlank(message = "eventType is required")
        private String eventType;

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
