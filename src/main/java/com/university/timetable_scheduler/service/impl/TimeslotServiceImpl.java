package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.timeslot.*;
import com.university.timetable_scheduler.dto.response.timeslot.*;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.entity.Timeslot;
import com.university.timetable_scheduler.mapper.TimeslotMapper;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.repository.TimeslotRepository;
import com.university.timetable_scheduler.service.TimeslotService;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
public class TimeslotServiceImpl implements TimeslotService {
    private final TimeslotRepository timeslotRepository;
    private final TimeslotMapper timeslotMapper;
    private final SchoolRepository schoolRepository;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    @Override
    public CreateTimeslotResponse createTimeslot(CreateTimeslotRequest request) {
        Timeslot entity = new Timeslot();
        entity.setSchool(currentSchool());
        entity.setTimeslotDay(request.getTimeslotDay());
        entity.setTimeslotStartTime(request.getTimeslotStartTime());
        entity.setTimeslotEndTime(request.getTimeslotEndTime());
        entity.setTimeslotDuration(request.getTimeslotDuration());
        Timeslot saved = timeslotRepository.save(entity);
        CreateTimeslotResponse response = new CreateTimeslotResponse();
        CreateTimeslotResponse.Data responseData = new CreateTimeslotResponse.Data();
        responseData.setTimeslot(timeslotMapper.toResponse(saved));
        response.setData(responseData);
        return response;
    }

    @Override
    public ReadTimeslotResponse readTimeslot(ReadTimeslotRequest request) {
        List<Timeslot> list = timeslotRepository.findTimeslotByFilter(
                TenantContext.getSchoolId(),
                request.getId(), request.getTimeslotDay(), request.getTimeslotStatus());
        ReadTimeslotResponse response = new ReadTimeslotResponse();
        ReadTimeslotResponse.Data responseData = new ReadTimeslotResponse.Data();
        responseData.setTimeslots(timeslotMapper.toResponseList(list));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public UpdateTimeslotResponse updateTimeslot(UpdateTimeslotRequest request) {
        Timeslot entity = timeslotRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeslot not found"));
        timeslotMapper.updateDtoToEntity(request, entity);
        UpdateTimeslotResponse response = new UpdateTimeslotResponse();
        UpdateTimeslotResponse.Data responseData = new UpdateTimeslotResponse.Data();
        responseData.setTimeslot(timeslotMapper.toResponse(entity));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public DeleteTimeslotResponse deleteTimeslot(DeleteTimeslotRequest request) {
        Timeslot entity = timeslotRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeslot not found"));
        entity.setIsDeleted(true);
        return new DeleteTimeslotResponse();
    }
}
