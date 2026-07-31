package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.response.section.SectionResponse;
import com.university.timetable_scheduler.entity.Section;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SectionMapper {
    @Mapping(source = "sectionCourse.id", target = "sectionCourseId")
    @Mapping(source = "sectionAcademicPeriod.id", target = "sectionAcademicPeriodId")
    SectionResponse toResponse(Section section);

    List<SectionResponse> toResponseList(List<Section> sections);
}

