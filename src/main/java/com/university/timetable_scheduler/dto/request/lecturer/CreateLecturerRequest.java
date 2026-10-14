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

    /** Optional, like the last name. Without either, the lecturer is shown by staff number. */
    private String lecturerFirstName;

    private String lecturerLastName;

    /** Optional; checked only when given. */
    @Email(message = "Email must be valid")
    private String lecturerEmail;

    @NotNull(message = "Department ID is required")
    private UUID lecturerDepartmentId;
}
