package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.academicperiod.*;
import com.university.timetable_scheduler.dto.response.academicperiod.*;
import com.university.timetable_scheduler.entity.AcademicPeriod;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.mapper.AcademicPeriodMapper;
import com.university.timetable_scheduler.repository.AcademicPeriodRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.service.AcademicPeriodService;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
public class AcademicPeriodServiceImpl implements AcademicPeriodService {
    private final AcademicPeriodRepository academicPeriodRepository;
    private final AcademicPeriodMapper academicPeriodMapper;
    private final SchoolRepository schoolRepository;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    @Override
    public CreateAcademicPeriodResponse createAcademicPeriod(CreateAcademicPeriodRequest request) {
        AcademicPeriod entity = new AcademicPeriod();
        entity.setSchool(currentSchool());
        entity.setAcademicPeriodName(request.getAcademicPeriodName());
        entity.setAcademicPeriodSession(request.getAcademicPeriodSession());
        entity.setAcademicPeriodSemester(request.getAcademicPeriodSemester());
        entity.setAcademicPeriodStartDate(request.getAcademicPeriodStartDate());
        entity.setAcademicPeriodEndDate(request.getAcademicPeriodEndDate());
        AcademicPeriod saved = academicPeriodRepository.save(entity);
        CreateAcademicPeriodResponse response = new CreateAcademicPeriodResponse();
        CreateAcademicPeriodResponse.Data responseData = new CreateAcademicPeriodResponse.Data();
        responseData.setAcademicPeriod(academicPeriodMapper.toResponse(saved));
        response.setData(responseData);
        return response;
    }

    @Override
    public ReadAcademicPeriodResponse readAcademicPeriod(ReadAcademicPeriodRequest request) {
        List<AcademicPeriod> list = academicPeriodRepository.findAcademicPeriodByFilter(
                TenantContext.getSchoolId(),
                request.getId(), request.getAcademicPeriodName(),
                request.getAcademicPeriodSession(), request.getAcademicPeriodSemester(),
                request.getAcademicPeriodStatus());
        ReadAcademicPeriodResponse response = new ReadAcademicPeriodResponse();
        ReadAcademicPeriodResponse.Data responseData = new ReadAcademicPeriodResponse.Data();
        responseData.setAcademicPeriods(academicPeriodMapper.toResponseList(list));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public UpdateAcademicPeriodResponse updateAcademicPeriod(UpdateAcademicPeriodRequest request) {
        AcademicPeriod entity = academicPeriodRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AcademicPeriod not found"));
        academicPeriodMapper.updateDtoToEntity(request, entity);
        UpdateAcademicPeriodResponse response = new UpdateAcademicPeriodResponse();
        UpdateAcademicPeriodResponse.Data responseData = new UpdateAcademicPeriodResponse.Data();
        responseData.setAcademicPeriod(academicPeriodMapper.toResponse(entity));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public DeleteAcademicPeriodResponse deleteAcademicPeriod(DeleteAcademicPeriodRequest request) {
        AcademicPeriod entity = academicPeriodRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AcademicPeriod not found"));
        entity.setIsDeleted(true);
        return new DeleteAcademicPeriodResponse();
    }
}