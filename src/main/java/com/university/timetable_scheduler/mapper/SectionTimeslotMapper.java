package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.response.sectiontimeslot.SectionTimeslotResponse;
import com.university.timetable_scheduler.entity.SectionTimeslot;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SectionTimeslotMapper {
    @Mapping(source = "sectionTimeslotSection.id", target = "sectionTimeslotSectionId")
    @Mapping(source = "sectionTimeslotTimeslot.id", target = "sectionTimeslotTimeslotId")
    SectionTimeslotResponse toResponse(SectionTimeslot sectionTimeslot);

    List<SectionTimeslotResponse> toResponseList(List<SectionTimeslot> sectionTimeslots);
}

