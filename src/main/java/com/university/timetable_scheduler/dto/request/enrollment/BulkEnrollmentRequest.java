package com.university.timetable_scheduler.dto.request.enrollment;

import com.university.timetable_scheduler.status.StudentEnum;
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

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BulkEnrollmentRequest {

    @NotNull(message = "Academic period ID is required")
    private UUID academicPeriodId;

    @NotEmpty(message = "Enrollment rows must not be empty")
    @Valid
    private List<Row> rows;

    @Schema(name = "BulkEnrollmentRow")
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Row {

        @NotBlank(message = "Matriculation number is required")
        private String studentMatriculationNumber;

        @NotBlank(message = "Student first name is required")
        private String studentFirstName;

        @NotBlank(message = "Student last name is required")
        private String studentLastName;

        @NotBlank(message = "Student email is required")
        @Email(message = "Email must be valid")
        private String studentEmail;

        @NotNull(message = "Student level is required")
        private StudentEnum.StudentLevel studentLevel;

        @NotBlank(message = "Department name is required")
        private String studentDepartment;

        @NotBlank(message = "Course section name is required")
        private String courseSection;
    }
}

