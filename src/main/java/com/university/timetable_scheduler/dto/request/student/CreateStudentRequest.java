package com.university.timetable_scheduler.dto.request.student;

import com.university.timetable_scheduler.status.StudentEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateStudentRequest {
    @NotBlank(message = "First name is required")
    private String studentFirstName;

    @NotBlank(message = "Last name is required")
    private String studentLastName;

    @NotBlank(message = "Matriculation number is required")
    private String studentMatriculationNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String studentEmail;

    @NotNull(message = "Student level is required")
    private StudentEnum.StudentLevel studentLevel;

    @NotNull(message = "Department ID is required")
    private UUID studentDepartmentId;
}
