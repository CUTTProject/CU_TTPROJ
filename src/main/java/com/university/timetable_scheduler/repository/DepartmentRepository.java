package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.Department;
import com.university.timetable_scheduler.status.DepartmentEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends TenantAwareRepository<Department> {

    @Query("""
        SELECT d FROM Department d
        WHERE d.school.id = :schoolId
          AND (d.isDeleted IS NULL OR d.isDeleted = false)
          AND (:id IS NULL OR d.id = :id)
          AND (:departmentName IS NULL OR d.departmentName = :departmentName)
          AND (:departmentStatus IS NULL OR d.departmentStatus = :departmentStatus)
    """)
    List<Department> findDepartmentByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("id") UUID id,
            @Param("departmentName") String departmentName,
            @Param("departmentStatus") DepartmentEnum.DepartmentStatus departmentStatus
    );
}
