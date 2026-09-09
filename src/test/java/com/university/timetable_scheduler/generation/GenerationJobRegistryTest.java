package com.university.timetable_scheduler.generation;

import com.university.timetable_scheduler.status.JobEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the single-flight rule and tenant scoping of job lookups.
 *
 * <p>Single-flight matters because two solvers on the same period would each write a full assignment
 * over the other's, and nothing in the result would say a race happened.
 */
class GenerationJobRegistryTest {

    private static final Instant START = Instant.parse("2026-09-09T10:00:00Z");

    private final UUID schoolA = UUID.randomUUID();
    private final UUID schoolB = UUID.randomUUID();
    private final UUID period = UUID.randomUUID();

    private Instant now;
    private GenerationJobRegistry registry;

    @BeforeEach
    void setUp() {
        now = START;
        Clock movable = new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now; }
        };
        registry = new GenerationJobRegistry(movable);
    }

    @Test
    @DisplayName("a second generation for the same period is refused while the first is running")
    void singleFlightPerPeriod() {
        GenerationJob first = registry.reserve(schoolA, period).orElseThrow();

        assertThat(registry.reserve(schoolA, period)).isEmpty();
        // A 409 must point at the job the caller already has.
        assertThat(registry.runningJobId(schoolA, period)).isEqualTo(first.getJobId());
    }

    @Test
    @DisplayName("releasing the claim allows the next generation")
    void releaseAllowsReuse() {
        registry.reserve(schoolA, period).orElseThrow();
        registry.release(schoolA, period);

        assertThat(registry.reserve(schoolA, period)).isPresent();
    }

    @Test
    @DisplayName("the claim is per school and per period, not global")
    void claimIsScoped() {
        registry.reserve(schoolA, period).orElseThrow();

        // Unrelated work must not block.
        assertThat(registry.reserve(schoolA, UUID.randomUUID())).isPresent();
        assertThat(registry.reserve(schoolB, period)).isPresent();
    }

    @Test
    @DisplayName("a job belonging to another school reads as absent, not forbidden")
    void lookupIsTenantScoped() {
        GenerationJob job = registry.reserve(schoolA, period).orElseThrow();

        assertThat(registry.find(job.getJobId(), schoolA)).isPresent();
        // Empty, not an error: a 403 would confirm it exists.
        assertThat(registry.find(job.getJobId(), schoolB)).isEmpty();
    }

    @Test
    @DisplayName("an unknown job id is simply absent")
    void unknownJob() {
        assertThat(registry.find(UUID.randomUUID(), schoolA)).isEmpty();
    }

    @Test
    @DisplayName("finished jobs are swept once their TTL expires; running ones are never swept")
    void terminalJobsExpire() {
        GenerationJob finished = registry.reserve(schoolA, period).orElseThrow();
        finished.markSucceeded(now);
        registry.release(schoolA, period);

        UUID otherPeriod = UUID.randomUUID();
        GenerationJob stillRunning = registry.reserve(schoolA, otherPeriod).orElseThrow();

        now = START.plus(Duration.ofHours(2));
        // The sweep runs on reserve.
        registry.reserve(schoolB, UUID.randomUUID()).orElseThrow();

        assertThat(registry.find(finished.getJobId(), schoolA)).isEmpty();
        Optional<GenerationJob> running = registry.find(stillRunning.getJobId(), schoolA);
        assertThat(running).isPresent();
        assertThat(running.get().getStatus()).isEqualTo(JobEnum.JobStatus.QUEUED);
    }

    @Test
    @DisplayName("a new job starts queued and is not yet terminal")
    void newJobState() {
        GenerationJob job = registry.reserve(schoolA, period).orElseThrow();

        assertThat(job.getStatus()).isEqualTo(JobEnum.JobStatus.QUEUED);
        assertThat(job.isTerminal()).isFalse();
        assertThat(job.getSubmittedAt()).isEqualTo(START);
        assertThat(job.getAcademicPeriodId()).isEqualTo(period);
    }
}
