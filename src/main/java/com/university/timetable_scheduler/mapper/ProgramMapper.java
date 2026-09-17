package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.request.program.UpdateProgramRequest;
import com.university.timetable_scheduler.dto.response.program.ProgramResponse;
import com.university.timetable_scheduler.entity.Lecturer;
import com.university.timetable_scheduler.entity.Program;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProgramMapper {

    /** {@code studentCount} is a derived aggregate the service fills in; see ProgramServiceImpl. */
    @Mapping(source = "programDepartment.id", target = "programDepartmentId")
    @Mapping(source = "programDepartment.departmentName", target = "programDepartmentName")
    @Mapping(source = "programCoordinator.id", target = "programCoordinatorId")
    @Mapping(target = "programCoordinatorName", expression = "java(coordinatorName(program))")
    @Mapping(target = "studentCount", ignore = true)
    ProgramResponse toResponse(Program program);

    List<ProgramResponse> toResponseList(List<Program> programs);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateDtoToEntity(UpdateProgramRequest request, @MappingTarget Program program);

    /** Joins whichever name parts the coordinator actually has. */
    default String coordinatorName(Program program) {
        Lecturer coordinator = program == null ? null : program.getProgramCoordinator();
        if (coordinator == null) {
            return null;
        }
        String name = Stream.of(coordinator.getLecturerFirstName(), coordinator.getLecturerLastName())
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        return name.isEmpty() ? null : name;
    }
}
