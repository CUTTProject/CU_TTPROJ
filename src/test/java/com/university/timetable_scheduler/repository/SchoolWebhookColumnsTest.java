package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.School;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the one schema property the webhook columns depend on: they are nullable.
 *
 * <p>{@code ddl-auto=update} adds them to a populated school table, and a NOT NULL column with no
 * default cannot be added there — the app would fail against any existing database while passing
 * every test that builds a fresh schema.
 */
@DataJpaTest
class SchoolWebhookColumnsTest {

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private EntityManager entityManager;

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

    @Test
    @DisplayName("a school persists with no webhook configured at all")
    void webhookColumnsAreNullable() {
        School saved = newSchool("no-webhook");
        entityManager.flush();
        entityManager.clear();

        School reloaded = schoolRepository.findLiveById(saved.getId()).orElseThrow();
        assertThat(reloaded.getWebhookUrl()).isNull();
        assertThat(reloaded.getWebhookSecret()).isNull();
        assertThat(reloaded.getWebhookEnabled()).isNull();
    }

    @Test
    @DisplayName("webhook settings round-trip, including a url at the documented length limit")
    void webhookColumnsRoundTrip() {
        School saved = newSchool("with-webhook");
        String longUrl = "https://example.com/" + "a".repeat(1900);
        saved.setWebhookUrl(longUrl);
        saved.setWebhookSecret("whsec_round_trip");
        saved.setWebhookEnabled(true);
        schoolRepository.save(saved);
        entityManager.flush();
        entityManager.clear();

        School reloaded = schoolRepository.findLiveById(saved.getId()).orElseThrow();
        assertThat(reloaded.getWebhookUrl()).isEqualTo(longUrl);
        assertThat(reloaded.getWebhookSecret()).isEqualTo("whsec_round_trip");
        assertThat(reloaded.getWebhookEnabled()).isTrue();
    }
}
