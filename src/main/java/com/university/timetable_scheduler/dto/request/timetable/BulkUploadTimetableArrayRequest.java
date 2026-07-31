package com.university.timetable_scheduler.dto.request.timetable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
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
 * JSON body for the array-based timetable bulk upload.
 *
 * Each row mirrors the fields of the CSV version ({@link BulkUploadTimetableFileRequest}).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BulkUploadTimetableArrayRequest {

    @NotNull(message = "academicPeriodId is required")
    private UUID academicPeriodId;

    @NotEmpty(message = "rows must not be empty")
    @Valid
    private List<Row> rows;

    @Schema(name = "BulkTimetableRow")
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Row {

        @NotBlank(message = "courseCode is required")
        private String courseCode;

        @NotBlank(message = "courseTitle is required")
        private String courseTitle;

        @NotNull(message = "courseUnit is required")
        private Integer courseUnit;

        /** Must match CourseEnum.CourseLevel e.g. LEVEL_100 */
        @NotBlank(message = "courseLevel is required")
        private String courseLevel;

        @NotBlank(message = "department is required")
        private String department;

        @NotBlank(message = "lecturerStaffNumber is required")
        private String lecturerStaffNumber;

        @NotBlank(message = "lecturerFirstName is required")
        private String lecturerFirstName;

        @NotBlank(message = "lecturerLastName is required")
        private String lecturerLastName;

        @NotBlank(message = "lecturerEmail is required")
        @Email(message = "lecturerEmail must be valid")
        private String lecturerEmail;

        @NotBlank(message = "sectionName is required")
        private String sectionName;

        @NotBlank(message = "sectionEnrollmentSize is required")
        private String sectionEnrollmentSize;


        /** Duration in minutes e.g. 60, 120 */
        @NotNull(message = "eventDurationMinutes is required")
        private Integer eventDurationMinutes;

        /** Must match EventEnum.EventType: CLASS or LAB */
        @NotBlank(message = "eventType is required")
        private String eventType;

        /**
         * Optional. Slash-separated room names.
         * Example: {@code Classroom1/Classroom2}
         */
        private String rooms;

        /**
         * Optional. Slash-separated timeslot entries in {@code DAY(HH:mm-HH:mm)} format.
         * Example: {@code M(13:00-15:00)/TH(14:00-17:00)}
         */
        private String timeslot;
    }
}

