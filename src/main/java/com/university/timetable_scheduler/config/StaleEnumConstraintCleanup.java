package com.university.timetable_scheduler.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Drops check constraints Hibernate generated for columns that are no longer mapped as enums.
 *
 * <p>{@code activity.activityType} was once {@code @Enumerated(EnumType.STRING)}, so databases
 * created then carry {@code CHECK (activity_type IN (...))} listing only the values of that time.
 * The column is now a plain String (see Activity), but {@code ddl-auto=update} never drops a
 * constraint, so every activity type added since fails to insert on those databases. Runs on
 * every start; once the constraint is gone it does nothing.
 */
@Slf4j
@Component
@AllArgsConstructor
public class StaleEnumConstraintCleanup implements ApplicationRunner {

    /** Table and constraint, as Hibernate named them on PostgreSQL. */
    static final List<String[]> STALE_CONSTRAINTS = List.<String[]>of(
            new String[]{"activity", "activity_activity_type_check"});

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        for (String[] stale : STALE_CONSTRAINTS) {
            String table = stale[0];
            String constraint = stale[1];
            Integer found = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.table_constraints
                    WHERE LOWER(table_name) = ? AND LOWER(constraint_name) = ?
                    """, Integer.class, table, constraint);
            if (found != null && found > 0) {
                jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT " + constraint);
                log.info("Dropped stale enum check constraint {} on {}", constraint, table);
            }
        }
    }
}
