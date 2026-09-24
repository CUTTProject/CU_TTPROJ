package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Development only: hard-deletes every record the caller's school owns, leaving the school and
 * its login so the data can be uploaded again straight away. Other schools are untouched.
 * Reachable only through TestingController, which refuses unless {@code testing.reset-enabled=true}.
 */
@Service
@AllArgsConstructor
public class TestDataResetService {

    /** Children before parents, so no foreign key is left pointing at a deleted row. */
    private static final List<String> ENTITIES_IN_DELETE_ORDER = List.of(
            "Enrollment", "Event", "SectionLecturer", "SectionRoom", "SectionTimeslot", "Section",
            "ProgramCourse", "Student", "AcademicPeriod", "Timeslot", "Room", "Course", "Program",
            "Lecturer", "Department", "Activity");

    private final EntityManager entityManager;

    /** @return rows deleted per entity, in the order they were deleted */
    @Transactional
    public Map<String, Integer> resetCurrentSchool() {
        UUID schoolId = TenantContext.getSchoolId();

        // Departments and lecturers reference each other; break the cycle first.
        entityManager.createQuery("UPDATE Department d SET d.departmentHead = null WHERE d.school.id = :schoolId")
                .setParameter("schoolId", schoolId)
                .executeUpdate();

        Map<String, Integer> deleted = new LinkedHashMap<>();
        for (String entity : ENTITIES_IN_DELETE_ORDER) {
            deleted.put(entity, entityManager.createQuery("DELETE FROM " + entity + " e WHERE e.school.id = :schoolId")
                    .setParameter("schoolId", schoolId)
                    .executeUpdate());
        }
        return deleted;
    }
}
