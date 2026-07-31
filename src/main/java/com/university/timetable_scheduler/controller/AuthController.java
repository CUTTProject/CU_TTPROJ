package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.auth.LoginRequest;
import com.university.timetable_scheduler.dto.response.auth.LoginResponse;
import com.university.timetable_scheduler.service.impl.AuthServiceImpl;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthServiceImpl authService;

    @PostMapping("/login")
    public LoginResponse login (@Valid @RequestBody LoginRequest loginRequest){
        return authService.loginService(loginRequest);
    }
}
