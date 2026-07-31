package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.Lecturer;
import com.university.timetable_scheduler.status.LecturerEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LecturerRepository extends TenantAwareRepository<Lecturer> {

    @Query("""
        SELECT l FROM Lecturer l
        WHERE l.school.id = :schoolId
          AND (l.isDeleted IS NULL OR l.isDeleted = false)
          AND (:id IS NULL OR l.id = :id)
          AND (:lecturerStaffNumber IS NULL OR l.lecturerStaffNumber = :lecturerStaffNumber)
          AND (:lecturerFirstName IS NULL OR l.lecturerFirstName = :lecturerFirstName)
          AND (:lecturerLastName IS NULL OR l.lecturerLastName = :lecturerLastName)
          AND (:lecturerEmail IS NULL OR l.lecturerEmail = :lecturerEmail)
          AND (:departmentId IS NULL OR l.lecturerDepartment.id = :departmentId)
          AND (:lecturerStatus IS NULL OR l.lecturerStatus = :lecturerStatus)
    """)
    List<Lecturer> findLecturerByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("id") UUID id,
            @Param("lecturerStaffNumber") String lecturerStaffNumber,
            @Param("lecturerFirstName") String lecturerFirstName,
            @Param("lecturerLastName") String lecturerLastName,
            @Param("lecturerEmail") String lecturerEmail,
            @Param("departmentId") UUID departmentId,
            @Param("lecturerStatus") LecturerEnum.LecturerStatus lecturerStatus
    );
}
