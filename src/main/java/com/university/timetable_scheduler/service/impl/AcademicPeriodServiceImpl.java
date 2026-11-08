package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.academicperiod.*;
import com.university.timetable_scheduler.dto.response.academicperiod.*;
import com.university.timetable_scheduler.entity.AcademicPeriod;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.mapper.AcademicPeriodMapper;
import com.university.timetable_scheduler.repository.AcademicPeriodRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.service.AcademicPeriodService;
import com.university.timetable_scheduler.status.AcademicPeriodEnum;
import com.university.timetable_scheduler.status.ActivityEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class AcademicPeriodServiceImpl implements AcademicPeriodService {
    private final AcademicPeriodRepository academicPeriodRepository;
    private final AcademicPeriodMapper academicPeriodMapper;
    private final SchoolRepository schoolRepository;
    private final ActivityServiceImpl activityService;
    private final AcademicPeriodGuard academicPeriodGuard;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    /** The end must come after the start, or the period would be over before it began. */
    private static void requireEndAfterStart(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "academicPeriodEndDate must be after academicPeriodStartDate");
        }
    }

    /** Only one period per school is ACTIVE: making one active closes all the others. */
    private void closeOthers(AcademicPeriod active) {
        academicPeriodRepository.deactivateAllExcept(active.getSchool().getId(), active.getId());
    }

    @Override
    @Transactional
    public CreateAcademicPeriodResponse createAcademicPeriod(CreateAcademicPeriodRequest request) {
        requireEndAfterStart(request.getAcademicPeriodStartDate(), request.getAcademicPeriodEndDate());
        AcademicPeriod entity = new AcademicPeriod();
        entity.setSchool(currentSchool());
        entity.setAcademicPeriodName(request.getAcademicPeriodName());
        entity.setAcademicPeriodSession(request.getAcademicPeriodSession());
        entity.setAcademicPeriodSemester(request.getAcademicPeriodSemester());
        entity.setAcademicPeriodStartDate(request.getAcademicPeriodStartDate());
        entity.setAcademicPeriodEndDate(request.getAcademicPeriodEndDate());
        AcademicPeriod saved = academicPeriodRepository.saveAndFlush(entity);
        closeOthers(saved);
        activityService.record(ActivityEnum.ActivityType.ACADEMIC_PERIOD_CREATED, "New academic period created",
                ActivityServiceImpl.label(saved.getAcademicPeriodName()) + " was created");
        CreateAcademicPeriodResponse response = new CreateAcademicPeriodResponse();
        CreateAcademicPeriodResponse.Data responseData = new CreateAcademicPeriodResponse.Data();
        responseData.setAcademicPeriod(academicPeriodMapper.toResponse(saved, academicPeriodGuard.now()));
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
        // The phase depends on the clock, so it is filtered here rather than in the query. A school
        // has a handful of periods, so this costs nothing.
        LocalDateTime now = academicPeriodGuard.now();
        if (request.getAcademicPeriodPhase() != null) {
            list = list.stream().filter(p -> p.phaseAt(now) == request.getAcademicPeriodPhase()).toList();
        }
        ReadAcademicPeriodResponse response = new ReadAcademicPeriodResponse();
        ReadAcademicPeriodResponse.Data responseData = new ReadAcademicPeriodResponse.Data();
        responseData.setAcademicPeriods(academicPeriodMapper.toResponseList(list, now));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public UpdateAcademicPeriodResponse updateAcademicPeriod(UpdateAcademicPeriodRequest request) {
        AcademicPeriod entity = academicPeriodRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AcademicPeriod not found"));
        // Not guarded: extending the end date or setting the status back to ACTIVE is how an
        // ended or archived period is reopened.
        academicPeriodMapper.updateDtoToEntity(request, entity);
        requireEndAfterStart(entity.getAcademicPeriodStartDate(), entity.getAcademicPeriodEndDate());
        if (request.getAcademicPeriodStatus() == AcademicPeriodEnum.AcademicPeriodStatus.ACTIVE) {
            entity = academicPeriodRepository.saveAndFlush(entity);
            closeOthers(entity);
        }
        UpdateAcademicPeriodResponse response = new UpdateAcademicPeriodResponse();
        UpdateAcademicPeriodResponse.Data responseData = new UpdateAcademicPeriodResponse.Data();
        responseData.setAcademicPeriod(academicPeriodMapper.toResponse(entity, academicPeriodGuard.now()));
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