package com.university.timetable_scheduler.dto.response.dashboard;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import com.university.timetable_scheduler.dto.response.academicperiod.AcademicPeriodResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DashboardStatsResponse extends BaseResponse {
    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "DashboardStatsResponseData")
    public static class Data {
        /** Null when the school has no active academic period. */
        private AcademicPeriodResponse currentAcademicPeriod;
        private long courses;
        private long lecturers;
        private long rooms;
        private long students;
    }
}
