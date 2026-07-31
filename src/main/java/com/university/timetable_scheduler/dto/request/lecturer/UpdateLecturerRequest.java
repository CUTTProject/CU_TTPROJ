package com.university.timetable_scheduler.dto.request.lecturer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class UpdateLecturerRequest {
    @NotNull(message = "Lecturer ID is required")
    private UUID id;

    private String lecturerStaffNumber;
    private String lecturerFirstName;
    private String lecturerLastName;

    @Email(message = "Email must be valid")
    private String lecturerEmail;

    private UUID lecturerDepartmentId;
}
