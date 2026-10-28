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

    /** The school's daily break (HH:mm), shown alongside the entries; null when it has none. */
    private String breakStartTime;

    private String breakEndTime;

    /** Events left without a time, each with the reason; listed under UNASSIGNED on the PDF. */
    private List<UnassignedEventDTO> unassigned;
}

