package com.university.timetable_scheduler.dto.response.timetable;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TimetableResponse extends BaseResponse {

    private List<TimetableEntryDTO> entries;

    private int totalEvents;

    private int scheduledEvents;
}

