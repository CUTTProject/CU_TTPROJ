package com.university.timetable_scheduler.dto.request.student;

import com.university.timetable_scheduler.status.StudentEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class UpdateStudentRequest {
    @NotNull(message = "Student ID is required")
    private UUID id;

    private String studentFirstName;
    private String studentLastName;
    private String studentMatriculationNumber;

    @Email(message = "Email must be valid")
    private String studentEmail;

    private StudentEnum.StudentLevel studentLevel;
    private UUID studentDepartmentId;
}
