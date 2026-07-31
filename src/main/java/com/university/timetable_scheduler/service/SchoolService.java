package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.school.*;
import com.university.timetable_scheduler.dto.response.school.*;

public interface SchoolService {
    CreateSchoolResponse createSchool(CreateSchoolRequest request);
    ReadSchoolResponse readSchool(ReadSchoolRequest request);
    UpdateSchoolResponse updateSchool(UpdateSchoolRequest request);
    DeleteSchoolResponse deleteSchool(DeleteSchoolRequest request);
}
