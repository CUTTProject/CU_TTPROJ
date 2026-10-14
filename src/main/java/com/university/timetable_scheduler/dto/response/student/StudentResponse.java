package com.university.timetable_scheduler.dto.response.student;

import java.util.UUID;

import com.university.timetable_scheduler.status.StudentEnum;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentResponse {
    private UUID id;
    private String studentFirstName;
    private String studentLastName;
    /** The name if the school supplied one, otherwise studentMatriculationNumber. Always set. */
    private String studentDisplayName;
    private String studentMatriculationNumber;
    private String studentEmail;
    private StudentEnum.StudentLevel studentLevel;
    private UUID studentDepartmentId;
    private UUID studentProgramId;
    private StudentEnum.StudentStatus studentStatus;
}

