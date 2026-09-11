package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.response.activity.ActivityResponse;
import com.university.timetable_scheduler.entity.Activity;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivityMapper {
    ActivityResponse toResponse(Activity activity);
    List<ActivityResponse> toResponseList(List<Activity> activities);
}
