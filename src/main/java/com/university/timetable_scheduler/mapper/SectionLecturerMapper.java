package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.response.sectionlecturer.SectionLecturerResponse;
import com.university.timetable_scheduler.entity.SectionLecturer;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SectionLecturerMapper {
    @Mapping(source = "sectionLecturerSection.id", target = "sectionLecturerSectionId")
    @Mapping(source = "sectionLecturerLecturer.id", target = "sectionLecturerLecturerId")
    SectionLecturerResponse toResponse(SectionLecturer sectionLecturer);

    List<SectionLecturerResponse> toResponseList(List<SectionLecturer> sectionLecturers);
}

