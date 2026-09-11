package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.response.dashboard.DashboardStatsResponse;

public interface DashboardService {

    DashboardStatsResponse readStats();

}
