package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.auth.LoginRequest;
import com.university.timetable_scheduler.dto.response.auth.LoginResponse;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.mapper.SchoolMapper;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.status.AuthEnum;
import com.university.timetable_scheduler.util.JwtUtil;
import io.jsonwebtoken.Jwt;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthServiceImpl {
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SchoolMapper schoolMapper;

    public LoginResponse loginService (LoginRequest loginRequest) {

        LoginResponse response = new LoginResponse();
        if (loginRequest.getLoginType() == AuthEnum.LoginType.SCHOOL){
            School school = schoolRepository.findSchoolByAdminEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School with " + loginRequest.getEmail() + "does not exist"));

            if (passwordEncoder.matches(loginRequest.getPassword(), school.getSchoolAdminPassword())){
                String authToken = jwtUtil.generateToken(new JwtUtil.JwtPayload(school.getId()));
                response.setAuthToken(authToken);
                response.setSchool(schoolMapper.toResponse(school));
                return response;
            }else{
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password");
            }
        }else {
            throw new NotImplementedException("Admin login not implemented");
        }
    }
}
