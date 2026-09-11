package com.university.timetable_scheduler.dto.response.activity;

import com.university.timetable_scheduler.status.ActivityEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ActivityResponse {
    private UUID id;
    private ActivityEnum.ActivityType activityType;
    private String activityTitle;
    private String activityDescription;
    private Instant activityOccurredAt;
}
