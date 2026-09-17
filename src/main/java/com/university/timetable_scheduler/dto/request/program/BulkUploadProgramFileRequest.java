package com.university.timetable_scheduler.dto.request.program;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;

/**
 * One CSV row of the programme bulk upload. Departments and coordinators are referenced by
 * their human-facing codes rather than UUIDs, since a spreadsheet author has no way to know
 * an internal id.
 */
@Getter
@Setter
public class BulkUploadProgramFileRequest {

    @CsvBindByName(column = "programName", required = true)
    private String programName;

    @CsvBindByName(column = "departmentCode", required = true)
    private String departmentCode;

    @CsvBindByName(column = "programCoordinatorStaffNumber")
    private String programCoordinatorStaffNumber;

    @CsvBindByName(column = "programLevel")
    private String programLevel;

    @CsvBindByName(column = "programDuration")
    private Integer programDuration;

    @CsvBindByName(column = "programDescription")
    private String programDescription;

    @CsvBindByName(column = "programStatus")
    private String programStatus;
}
