package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.academicperiod.*;
import com.university.timetable_scheduler.dto.response.academicperiod.*;

public interface AcademicPeriodService {
    CreateAcademicPeriodResponse createAcademicPeriod(CreateAcademicPeriodRequest request);
    ReadAcademicPeriodResponse readAcademicPeriod(ReadAcademicPeriodRequest request);
    UpdateAcademicPeriodResponse updateAcademicPeriod(UpdateAcademicPeriodRequest request);
    DeleteAcademicPeriodResponse deleteAcademicPeriod(DeleteAcademicPeriodRequest request);
}

