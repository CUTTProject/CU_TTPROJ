package com.university.timetable_scheduler.dto.request.student;

import java.util.UUID;

import com.university.timetable_scheduler.status.StudentEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadStudentRequest {
    private UUID id;
    private String studentFirstName;
    private String studentLastName;
    private String studentMatriculationNumber;
    private String studentEmail;
    private StudentEnum.StudentLevel studentLevel;
    private UUID studentDepartmentId;
    private StudentEnum.StudentStatus studentStatus;
}

