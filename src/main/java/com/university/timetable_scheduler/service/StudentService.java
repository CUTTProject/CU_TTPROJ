package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.student.*;
import com.university.timetable_scheduler.dto.response.student.*;

public interface StudentService {
    CreateStudentResponse createStudent(CreateStudentRequest request);
    ReadStudentResponse readStudent(ReadStudentRequest request);
    UpdateStudentResponse updateStudent(UpdateStudentRequest request);
    DeleteStudentResponse deleteStudent(DeleteStudentRequest request);
}

