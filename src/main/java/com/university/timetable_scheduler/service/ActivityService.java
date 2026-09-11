package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.activity.ReadActivityRequest;
import com.university.timetable_scheduler.dto.response.activity.ReadActivityResponse;
import com.university.timetable_scheduler.status.ActivityEnum;

public interface ActivityService {

    void record(ActivityEnum.ActivityType type, String title, String description);

    ReadActivityResponse readActivities(ReadActivityRequest request);

}
