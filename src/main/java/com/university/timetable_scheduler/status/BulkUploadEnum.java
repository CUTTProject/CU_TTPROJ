package com.university.timetable_scheduler.status;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class BulkUploadEnum {

    /**
     * The datasets that can be bulk uploaded, in the order a school has to upload them: each
     * one references rows the earlier ones create.
     */
    @Getter
    @AllArgsConstructor
    public enum BulkDataset {
        DEPARTMENTS("Department", "department", ActivityEnum.ActivityType.DEPARTMENTS_UPLOADED),
        LECTURERS("Lecturer", "lecturer", ActivityEnum.ActivityType.LECTURERS_UPLOADED),
        ROOMS("Room", "room", ActivityEnum.ActivityType.ROOMS_UPLOADED),
        PROGRAMS("Program", "program", ActivityEnum.ActivityType.PROGRAMS_UPLOADED),
        COURSES("Course", "course", ActivityEnum.ActivityType.COURSES_UPLOADED),
        CURRICULUM("Curriculum", "curriculum entry", ActivityEnum.ActivityType.CURRICULUM_UPLOADED),
        STUDENTS("Student", "student", ActivityEnum.ActivityType.STUDENTS_IMPORTED),
        SECTIONS("Section", "section event", ActivityEnum.ActivityType.SECTIONS_UPLOADED),
        ENROLLMENTS("Enrollment", "enrollment", ActivityEnum.ActivityType.ENROLLMENTS_UPLOADED);

        /** Capitalised, for the activity title: "Room data uploaded". */
        private final String label;
        /** Singular, for the activity description: "24 rooms were imported". */
        private final String noun;
        private final ActivityEnum.ActivityType activityType;
    }
}
