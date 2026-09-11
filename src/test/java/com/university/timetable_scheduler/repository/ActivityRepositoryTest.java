package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.Activity;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.status.ActivityEnum;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The dashboard feed: newest first, capped, and never another school's. */
@DataJpaTest
class ActivityRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-09-01T09:00:00Z");

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private EntityManager entityManager;

    private School schoolA;
    private School schoolB;

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

    private Activity newActivity(School school, String title, Instant occurredAt) {
        Activity activity = new Activity();
        activity.setSchool(school);
        activity.setActivityType(ActivityEnum.ActivityType.ROOMS_UPLOADED);
        activity.setActivityTitle(title);
        activity.setActivityDescription(title + " description");
        activity.setActivityOccurredAt(occurredAt);
        return activityRepository.save(activity);
    }

    @BeforeEach
    void setUp() {
        schoolA = newSchool("school-a");
        schoolB = newSchool("school-b");
    }

    @Test
    @DisplayName("returns the newest activities first, up to the limit")
    void newestFirstAndCapped() {
        // Saved out of order, so insertion order cannot pass for time order.
        newActivity(schoolA, "middle", T0.plus(Duration.ofHours(1)));
        newActivity(schoolA, "oldest", T0);
        newActivity(schoolA, "newest", T0.plus(Duration.ofHours(2)));
        entityManager.flush();
        entityManager.clear();

        List<Activity> recent = activityRepository.findRecent(schoolA.getId(), PageRequest.of(0, 2));

        assertThat(recent).extracting(Activity::getActivityTitle).containsExactly("newest", "middle");
    }

    @Test
    @DisplayName("never includes another school's activities, even newer ones")
    void scopedToTenant() {
        newActivity(schoolA, "a-only", T0);
        newActivity(schoolB, "b-newer", T0.plus(Duration.ofHours(5)));
        entityManager.flush();
        entityManager.clear();

        List<Activity> recent = activityRepository.findRecent(schoolA.getId(), PageRequest.of(0, 10));

        assertThat(recent).extracting(Activity::getActivityTitle).containsExactly("a-only");
    }

    @Test
    @DisplayName("excludes soft-deleted activities")
    void excludesDeleted() {
        newActivity(schoolA, "kept", T0);
        Activity deleted = newActivity(schoolA, "deleted", T0.plus(Duration.ofHours(1)));
        deleted.setIsDeleted(true);
        activityRepository.save(deleted);
        entityManager.flush();
        entityManager.clear();

        List<Activity> recent = activityRepository.findRecent(schoolA.getId(), PageRequest.of(0, 10));

        assertThat(recent).extracting(Activity::getActivityTitle).containsExactly("kept");
    }
}
