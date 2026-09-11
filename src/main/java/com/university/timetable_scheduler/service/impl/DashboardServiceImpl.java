package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.response.dashboard.DashboardStatsResponse;
import com.university.timetable_scheduler.entity.AcademicPeriod;
import com.university.timetable_scheduler.mapper.AcademicPeriodMapper;
import com.university.timetable_scheduler.repository.*;
import com.university.timetable_scheduler.service.DashboardService;
import com.university.timetable_scheduler.status.AcademicPeriodEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final AcademicPeriodRepository academicPeriodRepository;
    private final CourseRepository courseRepository;
    private final LecturerRepository lecturerRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final AcademicPeriodMapper academicPeriodMapper;
    private final Clock clock;

    @Override
    public DashboardStatsResponse readStats() {
        UUID schoolId = TenantContext.getSchoolId();

        DashboardStatsResponse.Data responseData = new DashboardStatsResponse.Data();
        responseData.setCurrentAcademicPeriod(academicPeriodMapper.toResponse(currentPeriod(schoolId)));
        responseData.setCourses(courseRepository.countLiveBySchoolId(schoolId));
        responseData.setLecturers(lecturerRepository.countLiveBySchoolId(schoolId));
        responseData.setRooms(roomRepository.countLiveBySchoolId(schoolId));
        responseData.setStudents(studentRepository.countLiveBySchoolId(schoolId));

        DashboardStatsResponse response = new DashboardStatsResponse();
        response.setData(responseData);
        return response;
    }

    /**
     * The active period whose dates contain now; failing that, the active period that started
     * most recently. Every period is created ACTIVE, so status alone cannot pick one.
     */
    private AcademicPeriod currentPeriod(UUID schoolId) {
        List<AcademicPeriod> active = academicPeriodRepository.findAcademicPeriodByFilter(
                schoolId, null, null, null, null, AcademicPeriodEnum.AcademicPeriodStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now(clock);
        Comparator<AcademicPeriod> latestStart = Comparator.comparing(
                AcademicPeriod::getAcademicPeriodStartDate, Comparator.nullsFirst(Comparator.naturalOrder()));

        return active.stream()
                .filter(p -> p.getAcademicPeriodStartDate() != null && p.getAcademicPeriodEndDate() != null
                        && !now.isBefore(p.getAcademicPeriodStartDate())
                        && !now.isAfter(p.getAcademicPeriodEndDate()))
                .max(latestStart)
                .orElseGet(() -> active.stream().max(latestStart).orElse(null));
    }
}
