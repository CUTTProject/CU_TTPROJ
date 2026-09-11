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
          AND (:departmentCode IS NULL OR UPPER(d.departmentCode) = UPPER(:departmentCode))
          AND (:departmentStatus IS NULL OR d.departmentStatus = :departmentStatus)
    """)
    List<Department> findDepartmentByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("id") UUID id,
            @Param("departmentName") String departmentName,
            @Param("departmentCode") String departmentCode,
            @Param("departmentStatus") DepartmentEnum.DepartmentStatus departmentStatus
    );

    /** True if another live department in the school already uses this code (case-insensitive). */
    @Query("""
        SELECT COUNT(d) > 0 FROM Department d
        WHERE d.school.id = :schoolId
          AND (d.isDeleted IS NULL OR d.isDeleted = false)
          AND UPPER(d.departmentCode) = UPPER(:departmentCode)
          AND (:excludeId IS NULL OR d.id <> :excludeId)
    """)
    boolean existsLiveByDepartmentCode(
            @Param("schoolId") UUID schoolId,
            @Param("departmentCode") String departmentCode,
            @Param("excludeId") UUID excludeId
    );
}
