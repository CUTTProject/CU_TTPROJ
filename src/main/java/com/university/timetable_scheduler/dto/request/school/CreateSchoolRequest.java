package com.university.timetable_scheduler.dto.request.school;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateSchoolRequest {
    @NotBlank(message = "School name is required")
    private String schoolName;

    @NotBlank(message = "School address is required")
    private String schoolAddress;

    @NotBlank(message = "School admin email is required")
    @Email(message = "Email must be valid")
    private String schoolAdminEmail;

    @NotBlank(message = "School admin password is required")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character"
    )
    private String schoolAdminPassword;

    @NotBlank(message = "School phone is required")
    private String schoolPhone;

    @NotNull(message = "SchoolDayStartHour is required")
    private LocalTime schoolDayStartHour;

    @NotNull(message = "SchoolDayEndHour is required")
    private LocalTime schoolDayEndHour;
}

