package com.university.timetable_scheduler.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Recreates what a database from before the String mapping has: the old enum check constraint,
 * listing only the activity types of that time.
 */
@SpringBootTest
class StaleEnumConstraintCleanupTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private StaleEnumConstraintCleanup cleanup;

    private int constraintCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE LOWER(table_name) = 'activity' AND LOWER(constraint_name) = 'activity_activity_type_check'
                """, Integer.class);
    }

    @Test
    @DisplayName("drops the old activity_type check, and does nothing when it is already gone")
    void dropsStaleConstraint() {
        jdbcTemplate.execute("ALTER TABLE activity ADD CONSTRAINT activity_activity_type_check "
                + "CHECK (activity_type IN ('COURSE_CREATED', 'ROOM_CREATED'))");
        assertThat(constraintCount()).isEqualTo(1);

        cleanup.run(null);

        assertThat(constraintCount()).isZero();
        assertThatCode(() -> cleanup.run(null)).doesNotThrowAnyException();
    }
}
