package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.sectiontimeslot.*;
import com.university.timetable_scheduler.dto.response.sectiontimeslot.*;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.entity.Section;
import com.university.timetable_scheduler.entity.SectionTimeslot;
import com.university.timetable_scheduler.entity.Timeslot;
import com.university.timetable_scheduler.mapper.SectionTimeslotMapper;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.repository.SectionRepository;
import com.university.timetable_scheduler.repository.SectionTimeslotRepository;
import com.university.timetable_scheduler.repository.TimeslotRepository;
import com.university.timetable_scheduler.service.SectionTimeslotService;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
public class SectionTimeslotServiceImpl implements SectionTimeslotService {
    private final SectionTimeslotRepository sectionTimeslotRepository;
    private final SectionRepository sectionRepository;
    private final TimeslotRepository timeslotRepository;
    private final SectionTimeslotMapper sectionTimeslotMapper;
    private final SchoolRepository schoolRepository;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    @Override
    public CreateSectionTimeslotResponse createSectionTimeslot(CreateSectionTimeslotRequest request) {
        Section section = sectionRepository.findByIdAndSchoolId(request.getSectionTimeslotSectionId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        Timeslot timeslot = timeslotRepository.findByIdAndSchoolId(request.getSectionTimeslotTimeslotId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeslot not found"));
        SectionTimeslot entity = new SectionTimeslot();
        entity.setSchool(currentSchool());
        entity.setSectionTimeslotSection(section);
        entity.setSectionTimeslotTimeslot(timeslot);
        SectionTimeslot saved = sectionTimeslotRepository.save(entity);
        CreateSectionTimeslotResponse response = new CreateSectionTimeslotResponse();
        CreateSectionTimeslotResponse.Data responseData = new CreateSectionTimeslotResponse.Data();
        responseData.setSectionTimeslot(sectionTimeslotMapper.toResponse(saved));
        response.setData(responseData);
        return response;
    }

    @Override
    public ReadSectionTimeslotResponse readSectionTimeslot(ReadSectionTimeslotRequest request) {
        List<SectionTimeslot> list = sectionTimeslotRepository.findSectionTimeslotByFilter(
                TenantContext.getSchoolId(),
                request.getId(), request.getSectionTimeslotSectionId(), request.getSectionTimeslotTimeslotId());
        ReadSectionTimeslotResponse response = new ReadSectionTimeslotResponse();
        ReadSectionTimeslotResponse.Data responseData = new ReadSectionTimeslotResponse.Data();
        responseData.setSectionTimeslots(sectionTimeslotMapper.toResponseList(list));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public UpdateSectionTimeslotResponse updateSectionTimeslot(UpdateSectionTimeslotRequest request) {
        SectionTimeslot entity = sectionTimeslotRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SectionTimeslot not found"));
        if (request.getSectionTimeslotSectionId() != null) {
            Section section = sectionRepository.findByIdAndSchoolId(request.getSectionTimeslotSectionId(), TenantContext.getSchoolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
            entity.setSectionTimeslotSection(section);
        }
        if (request.getSectionTimeslotTimeslotId() != null) {
            Timeslot timeslot = timeslotRepository.findByIdAndSchoolId(request.getSectionTimeslotTimeslotId(), TenantContext.getSchoolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeslot not found"));
            entity.setSectionTimeslotTimeslot(timeslot);
        }
        UpdateSectionTimeslotResponse response = new UpdateSectionTimeslotResponse();
        UpdateSectionTimeslotResponse.Data responseData = new UpdateSectionTimeslotResponse.Data();
        responseData.setSectionTimeslot(sectionTimeslotMapper.toResponse(entity));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public DeleteSectionTimeslotResponse deleteSectionTimeslot(DeleteSectionTimeslotRequest request) {
        SectionTimeslot entity = sectionTimeslotRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SectionTimeslot not found"));
        entity.setIsDeleted(true);
        return new DeleteSectionTimeslotResponse();
    }
}
