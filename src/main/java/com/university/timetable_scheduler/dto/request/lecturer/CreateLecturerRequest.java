package com.university.timetable_scheduler.dto.request.lecturer;

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
public class CreateLecturerRequest {
    @NotBlank(message = "Staff number is required")
    private String lecturerStaffNumber;

    @NotBlank(message = "First name is required")
    private String lecturerFirstName;

    @NotBlank(message = "Last name is required")
    private String lecturerLastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String lecturerEmail;

    @NotNull(message = "Department ID is required")
    private UUID lecturerDepartmentId;
}
