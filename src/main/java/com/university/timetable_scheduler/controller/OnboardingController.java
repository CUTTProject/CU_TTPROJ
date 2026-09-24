package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.response.onboarding.OnboardingGuideResponse;
import com.university.timetable_scheduler.service.impl.OnboardingServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding")
@AllArgsConstructor
public class OnboardingController {
    private final OnboardingServiceImpl onboardingService;

    @Operation(summary = "The steps to set a school up, in order: each bulk upload with its endpoint, CSV "
            + "columns and dependencies, then creating an academic period and generating the timetable. "
            + "Each step says whether the caller's school has done it and whether it is ready to do.")
    @GetMapping("/guide")
    public OnboardingGuideResponse readGuide() {
        return onboardingService.readGuide();
    }
}
