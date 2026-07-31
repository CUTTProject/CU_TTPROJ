package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.request.department.UpdateDepartmentRequest;
import com.university.timetable_scheduler.dto.response.department.DepartmentResponse;
import com.university.timetable_scheduler.entity.Department;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DepartmentMapper {
    DepartmentResponse toResponse(Department department);
    List<DepartmentResponse> toResponseList(List<Department> departments);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateDtoToEntity(UpdateDepartmentRequest request, @MappingTarget Department department);
}

