package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.response.dashboard.DashboardStatsResponse;
import com.university.timetable_scheduler.service.impl.DashboardServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@AllArgsConstructor
public class DashboardController {
    private final DashboardServiceImpl dashboardService;

    @Operation(summary = "Headline figures for the dashboard: the current academic period "
            + "and live counts of courses, lecturers, rooms and students for the caller's school.")
    @GetMapping("/stats")
    public DashboardStatsResponse readStats() {
        return dashboardService.readStats();
    }
}
