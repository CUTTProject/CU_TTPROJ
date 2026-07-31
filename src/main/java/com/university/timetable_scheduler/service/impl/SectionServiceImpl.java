package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.section.*;
import com.university.timetable_scheduler.dto.response.section.*;
import com.university.timetable_scheduler.entity.AcademicPeriod;
import com.university.timetable_scheduler.entity.Course;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.entity.Section;
import com.university.timetable_scheduler.mapper.SectionMapper;
import com.university.timetable_scheduler.repository.AcademicPeriodRepository;
import com.university.timetable_scheduler.repository.CourseRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.repository.SectionRepository;
import com.university.timetable_scheduler.service.SectionService;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
public class SectionServiceImpl implements SectionService {
    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final SectionMapper sectionMapper;
    private final SchoolRepository schoolRepository;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    @Override
    public CreateSectionResponse createSection(CreateSectionRequest request) {
        Course course = courseRepository.findByIdAndSchoolId(request.getSectionCourseId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        AcademicPeriod academicPeriod = academicPeriodRepository.findByIdAndSchoolId(request.getSectionAcademicPeriodId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AcademicPeriod not found"));
        Section entity = new Section();
        entity.setSchool(currentSchool());
        entity.setSectionCourse(course);
        entity.setSectionName(request.getSectionName());
        entity.setSectionEnrollmentSize(request.getSectionEnrollmentSize());
        entity.setSectionAcademicPeriod(academicPeriod);
        Section saved = sectionRepository.save(entity);
        CreateSectionResponse response = new CreateSectionResponse();
        CreateSectionResponse.Data responseData = new CreateSectionResponse.Data();
        responseData.setSection(sectionMapper.toResponse(saved));
        response.setData(responseData);
        return response;
    }

    @Override
    public ReadSectionResponse readSection(ReadSectionRequest request) {
        List<Section> list = sectionRepository.findSectionByFilter(
                TenantContext.getSchoolId(),
                request.getId(), request.getSectionCourseId(), request.getSectionName(),
                request.getSectionAcademicPeriodId(), request.getSectionStatus());
        ReadSectionResponse response = new ReadSectionResponse();
        ReadSectionResponse.Data responseData = new ReadSectionResponse.Data();
        responseData.setSections(sectionMapper.toResponseList(list));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public UpdateSectionResponse updateSection(UpdateSectionRequest request) {
        Section entity = sectionRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        if (request.getSectionName() != null) entity.setSectionName(request.getSectionName());
        if (request.getSectionEnrollmentSize() != null) entity.setSectionEnrollmentSize(request.getSectionEnrollmentSize());
        if (request.getSectionCourseId() != null) {
            Course course = courseRepository.findByIdAndSchoolId(request.getSectionCourseId(), TenantContext.getSchoolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
            entity.setSectionCourse(course);
        }
        if (request.getSectionAcademicPeriodId() != null) {
            AcademicPeriod academicPeriod = academicPeriodRepository.findByIdAndSchoolId(request.getSectionAcademicPeriodId(), TenantContext.getSchoolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AcademicPeriod not found"));
            entity.setSectionAcademicPeriod(academicPeriod);
        }
        UpdateSectionResponse response = new UpdateSectionResponse();
        UpdateSectionResponse.Data responseData = new UpdateSectionResponse.Data();
        responseData.setSection(sectionMapper.toResponse(entity));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public DeleteSectionResponse deleteSection(DeleteSectionRequest request) {
        Section entity = sectionRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        entity.setIsDeleted(true);
        return new DeleteSectionResponse();
    }
}
