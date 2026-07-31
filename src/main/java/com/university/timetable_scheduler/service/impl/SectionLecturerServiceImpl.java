package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.sectionlecturer.*;
import com.university.timetable_scheduler.dto.response.sectionlecturer.*;
import com.university.timetable_scheduler.entity.Lecturer;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.entity.Section;
import com.university.timetable_scheduler.entity.SectionLecturer;
import com.university.timetable_scheduler.mapper.SectionLecturerMapper;
import com.university.timetable_scheduler.repository.LecturerRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.repository.SectionLecturerRepository;
import com.university.timetable_scheduler.repository.SectionRepository;
import com.university.timetable_scheduler.service.SectionLecturerService;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
public class SectionLecturerServiceImpl implements SectionLecturerService {
    private final SectionLecturerRepository sectionLecturerRepository;
    private final SectionRepository sectionRepository;
    private final LecturerRepository lecturerRepository;
    private final SectionLecturerMapper sectionLecturerMapper;
    private final SchoolRepository schoolRepository;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    @Override
    public CreateSectionLecturerResponse createSectionLecturer(CreateSectionLecturerRequest request) {
        Section section = sectionRepository.findByIdAndSchoolId(request.getSectionLecturerSectionId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        Lecturer lecturer = lecturerRepository.findByIdAndSchoolId(request.getSectionLecturerLecturerId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lecturer not found"));
        SectionLecturer entity = new SectionLecturer();
        entity.setSchool(currentSchool());
        entity.setSectionLecturerSection(section);
        entity.setSectionLecturerLecturer(lecturer);
        SectionLecturer saved = sectionLecturerRepository.save(entity);
        CreateSectionLecturerResponse response = new CreateSectionLecturerResponse();
        CreateSectionLecturerResponse.Data responseData = new CreateSectionLecturerResponse.Data();
        responseData.setSectionLecturer(sectionLecturerMapper.toResponse(saved));
        response.setData(responseData);
        return response;
    }

    @Override
    public ReadSectionLecturerResponse readSectionLecturer(ReadSectionLecturerRequest request) {
        List<SectionLecturer> list = sectionLecturerRepository.findSectionLecturerByFilter(
                TenantContext.getSchoolId(),
                request.getId(), request.getSectionLecturerSectionId(), request.getSectionLecturerLecturerId());
        ReadSectionLecturerResponse response = new ReadSectionLecturerResponse();
        ReadSectionLecturerResponse.Data responseData = new ReadSectionLecturerResponse.Data();
        responseData.setSectionLecturers(sectionLecturerMapper.toResponseList(list));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public UpdateSectionLecturerResponse updateSectionLecturer(UpdateSectionLecturerRequest request) {
        SectionLecturer entity = sectionLecturerRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SectionLecturer not found"));
        if (request.getSectionLecturerSectionId() != null) {
            Section section = sectionRepository.findByIdAndSchoolId(request.getSectionLecturerSectionId(), TenantContext.getSchoolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
            entity.setSectionLecturerSection(section);
        }
        if (request.getSectionLecturerLecturerId() != null) {
            Lecturer lecturer = lecturerRepository.findByIdAndSchoolId(request.getSectionLecturerLecturerId(), TenantContext.getSchoolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lecturer not found"));
            entity.setSectionLecturerLecturer(lecturer);
        }
        UpdateSectionLecturerResponse response = new UpdateSectionLecturerResponse();
        UpdateSectionLecturerResponse.Data responseData = new UpdateSectionLecturerResponse.Data();
        responseData.setSectionLecturer(sectionLecturerMapper.toResponse(entity));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public DeleteSectionLecturerResponse deleteSectionLecturer(DeleteSectionLecturerRequest request) {
        SectionLecturer entity = sectionLecturerRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SectionLecturer not found"));
        entity.setIsDeleted(true);
        return new DeleteSectionLecturerResponse();
    }
}
