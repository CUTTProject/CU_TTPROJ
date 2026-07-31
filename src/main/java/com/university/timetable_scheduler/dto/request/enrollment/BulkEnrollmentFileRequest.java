package com.university.timetable_scheduler.dto.request.enrollment;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;

/**
 * One CSV row for bulk enrollment.
 *
 * Expected CSV header (case-insensitive):
 * studentMatriculationNumber, studentFirstName, studentLastName,
 * studentEmail, studentLevel, studentDepartment, courseSection
 */
@Getter
@Setter
public class BulkEnrollmentFileRequest {

    @CsvBindByName(column = "studentMatriculationNumber", required = true)
    private String studentMatriculationNumber;

    @CsvBindByName(column = "studentFirstName", required = true)
    private String studentFirstName;

    @CsvBindByName(column = "studentLastName", required = true)
    private String studentLastName;

    @CsvBindByName(column = "studentEmail", required = true)
    private String studentEmail;

    /** Must match StudentEnum.StudentLevel e.g. LEVEL_100 */
    @CsvBindByName(column = "studentLevel", required = true)
    private String studentLevel;

    @CsvBindByName(column = "studentDepartment", required = true)
    private String studentDepartment;

    @CsvBindByName(column = "courseSection", required = true)
    private String courseSection;
}

