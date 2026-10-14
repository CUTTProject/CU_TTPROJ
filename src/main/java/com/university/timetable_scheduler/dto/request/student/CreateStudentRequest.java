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
    /** Optional, like the last name. Without either, the student is shown by matriculation number. */
    private String studentFirstName;

    private String studentLastName;

    @NotBlank(message = "Matriculation number is required")
    private String studentMatriculationNumber;

    /** Optional; checked only when given. */
    @Email(message = "Email must be valid")
    private String studentEmail;

    @NotNull(message = "Student level is required")
    private StudentEnum.StudentLevel studentLevel;

    @NotNull(message = "Department ID is required")
    private UUID studentDepartmentId;

    /** Optional, so existing callers that predate programmes keep working. */
    private UUID studentProgramId;
}
