package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.activity.ReadActivityRequest;
import com.university.timetable_scheduler.dto.response.activity.ReadActivityResponse;
import com.university.timetable_scheduler.entity.Activity;
import com.university.timetable_scheduler.mapper.ActivityMapper;
import com.university.timetable_scheduler.repository.ActivityRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.service.ActivityService;
import com.university.timetable_scheduler.status.ActivityEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ActivityServiceImpl implements ActivityService {
    private static final int DEFAULT_LIMIT = 10;

    private final ActivityRepository activityRepository;
    private final SchoolRepository schoolRepository;
    private final ActivityMapper activityMapper;
    private final Clock clock;

    /**
     * Records against the tenant bound to this thread. Joins the caller's transaction when there is
     * one, so an upload that rolls back leaves no "uploaded successfully" behind.
     */
    @Override
    public void record(ActivityEnum.ActivityType type, String title, String description) {
        Activity activity = new Activity();
        activity.setSchool(schoolRepository.getReferenceById(TenantContext.getSchoolId()));
        activity.setActivityType(type);
        activity.setActivityTitle(title);
        activity.setActivityDescription(description);
        activity.setActivityOccurredAt(Instant.now(clock));
        activityRepository.save(activity);
    }

    @Override
    public ReadActivityResponse readActivities(ReadActivityRequest request) {
        int limit = request.getLimit() != null ? request.getLimit() : DEFAULT_LIMIT;
        List<Activity> list = activityRepository.findRecent(TenantContext.getSchoolId(), PageRequest.of(0, limit));
        ReadActivityResponse response = new ReadActivityResponse();
        ReadActivityResponse.Data responseData = new ReadActivityResponse.Data();
        responseData.setActivities(activityMapper.toResponseList(list));
        response.setData(responseData);
        return response;
    }

    /** "1 room was imported", "24 rooms were imported". */
    public static String imported(int count, String noun) {
        return count == 1 ? "1 " + noun + " was imported" : count + " " + noun + "s were imported";
    }

    /** Joins the non-blank parts with spaces, for names that may be partly missing. */
    public static String label(String... parts) {
        return Arrays.stream(parts)
                .filter(p -> p != null && !p.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }
}
