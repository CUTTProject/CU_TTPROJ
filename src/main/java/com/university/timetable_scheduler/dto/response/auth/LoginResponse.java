package com.university.timetable_scheduler.dto.response.auth;

import com.university.timetable_scheduler.dto.response.school.SchoolResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LoginResponse {
    private SchoolResponse school;
    private String authToken;
}
