package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.lecturer.*;
import com.university.timetable_scheduler.dto.response.lecturer.*;

public interface LecturerService {
    CreateLecturerResponse createLecturer(CreateLecturerRequest request);
    ReadLecturerResponse readLecturer(ReadLecturerRequest request);
    UpdateLecturerResponse updateLecturer(UpdateLecturerRequest request);
    DeleteLecturerResponse deleteLecturer(DeleteLecturerRequest request);
}

