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
    private String studentMatriculationNumber;
    private String studentEmail;
    private StudentEnum.StudentLevel studentLevel;
    private UUID studentDepartmentId;
    private StudentEnum.StudentStatus studentStatus;
}

