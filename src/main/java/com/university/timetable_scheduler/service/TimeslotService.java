package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.timeslot.*;
import com.university.timetable_scheduler.dto.response.timeslot.*;

public interface TimeslotService {
    CreateTimeslotResponse createTimeslot(CreateTimeslotRequest request);
    ReadTimeslotResponse readTimeslot(ReadTimeslotRequest request);
    UpdateTimeslotResponse updateTimeslot(UpdateTimeslotRequest request);
    DeleteTimeslotResponse deleteTimeslot(DeleteTimeslotRequest request);
}

