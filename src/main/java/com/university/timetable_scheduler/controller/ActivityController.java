package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.activity.ReadActivityRequest;
import com.university.timetable_scheduler.dto.response.activity.ReadActivityResponse;
import com.university.timetable_scheduler.service.impl.ActivityServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activities")
@AllArgsConstructor
public class ActivityController {
    private final ActivityServiceImpl activityService;

    @Operation(summary = "The caller's school's most recent activities, newest first. "
            + "'limit' is 1-50 and defaults to 10.")
    @GetMapping("/read")
    public ReadActivityResponse readActivities(@Valid @ModelAttribute ReadActivityRequest request) {
        return activityService.readActivities(request);
    }
}
