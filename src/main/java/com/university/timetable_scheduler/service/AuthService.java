package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.auth.LoginRequest;
import com.university.timetable_scheduler.dto.response.auth.LoginResponse;

public interface AuthService {
    LoginResponse loginService (LoginRequest loginRequest);
}
