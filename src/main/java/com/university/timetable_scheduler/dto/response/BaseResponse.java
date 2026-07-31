package com.university.timetable_scheduler.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.Instant;


@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class BaseResponse {
    private String responseCode= HttpStatus.OK.toString();

    private Boolean error = false;

    private String timestamp = Instant.now().toString();

    private String path;

    private String responseMessage = "Success";
}


