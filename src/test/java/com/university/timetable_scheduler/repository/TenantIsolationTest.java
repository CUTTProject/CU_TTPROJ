package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.Course;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.status.CourseEnum;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the tenant-isolation contract of {@link TenantAwareRepository}.
 *
 * <p>These tests exist because the bug they cover was invisible: reads were scoped via the
 * findXByFilter queries, but every update/delete path used the inherited, unscoped
 * {@code findById}, so school A could mutate school B's rows. Course stands in for all
 * tenant-aware entities — they share the same base repository and the same query.
 */
@DataJpaTest
class TenantIsolationTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private EntityManager entityManager;

    private School schoolA;
    private School schoolB;
    private Course courseOfA;

    private School newSchool(String name) {
        School school = new School();
        school.setSchoolName(name);
        school.setSchoolAdminEmail("admin@" + name + ".test");
        school.setSchoolAdminPassword("irrelevant-hash");
        school.setSchoolAddress(name + " address");
        school.setSchoolPhone("phone-" + name);
        school.setSchoolDayStartHour(LocalTime.of(8, 0));
        school.setSchoolDayEndHour(LocalTime.of(18, 0));
        return schoolRepository.save(school);
    }

    private Course newCourse(School school, String code) {
        Course course = new Course();
        course.setSchool(school);
        course.setCourseCode(code);
        course.setCourseName(code + " name");
        course.setCourseUnit(3);
        course.setCourseLevel(CourseEnum.CourseLevel.LEVEL_100);
        return courseRepository.save(course);
    }

    @BeforeEach
    void setUp() {
        schoolA = newSchool("school-a");
        schoolB = newSchool("school-b");
        courseOfA = newCourse(schoolA, "CSC101");
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("a school can load its own row by id")
    void findsOwnRow() {
        Optional<Course> found = courseRepository.findByIdAndSchoolId(courseOfA.getId(), schoolA.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCourseCode()).isEqualTo("CSC101");
    }

    @Test
    @DisplayName("a school cannot load another school's row by id — the IDOR that update/delete relied on")
    void cannotFindAnotherSchoolsRow() {
        Optional<Course> found = courseRepository.findByIdAndSchoolId(courseOfA.getId(), schoolB.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("soft-deleted rows are not returned by id")
    void softDeletedRowIsNotFoundById() {
        Course course = courseRepository.findByIdAndSchoolId(courseOfA.getId(), schoolA.getId()).orElseThrow();
        course.setIsDeleted(true);
        courseRepository.save(course);
        entityManager.flush();
        entityManager.clear();

        assertThat(courseRepository.findByIdAndSchoolId(courseOfA.getId(), schoolA.getId())).isEmpty();
    }

    @Test
    @DisplayName("findAllBySchool_Id returns only the caller's live rows")
    void listIsScopedToTenantAndExcludesDeleted() {
        newCourse(schoolB, "MTH101");
        Course deletedOfA = newCourse(schoolA, "PHY101");
        deletedOfA.setIsDeleted(true);
        courseRepository.save(deletedOfA);
        entityManager.flush();
        entityManager.clear();

        List<Course> aCourses = courseRepository.findAllBySchool_Id(schoolA.getId());
        assertThat(aCourses).extracting(Course::getCourseCode).containsExactly("CSC101");

        List<Course> bCourses = courseRepository.findAllBySchool_Id(schoolB.getId());
        assertThat(bCourses).extracting(Course::getCourseCode).containsExactly("MTH101");
    }

    @Test
    @DisplayName("findCourseByFilter with no filters returns only the caller's live rows")
    void filterQueryIsScopedToTenant() {
        newCourse(schoolB, "MTH101");
        entityManager.flush();
        entityManager.clear();

        List<Course> aCourses = courseRepository.findCourseByFilter(
                schoolA.getId(), null, null, null, null, null, null);
        assertThat(aCourses).extracting(Course::getCourseCode).containsExactly("CSC101");
    }

    @Test
    @DisplayName("a school cannot reach another school's row even by naming its id in a filter")
    void filterQueryCannotReachAcrossTenants() {
        List<Course> found = courseRepository.findCourseByFilter(
                schoolB.getId(), courseOfA.getId(), null, null, null, null, null);
        assertThat(found).isEmpty();
    }
}
