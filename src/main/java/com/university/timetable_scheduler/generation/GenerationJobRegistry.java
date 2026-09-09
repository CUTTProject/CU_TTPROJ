package com.university.timetable_scheduler.generation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks generation jobs in memory and allows one running generation per academic period.
 *
 * <p>Memory rather than a table because the run's real output is already durable on the Event rows;
 * a job record only says what is happening now. A persisted {@code RUNNING} row would also survive
 * the restart that killed its thread and stay that way without a reaper. The cost is that a status
 * lookup 404s after a restart — the message says where to read the timetable instead.
 *
 * <p>The in-flight guard is per-JVM. Scaling out would need a database advisory lock.
 */
@Slf4j
@Component
public class GenerationJobRegistry {

    /** How long a finished job stays readable. */
    private static final Duration TERMINAL_TTL = Duration.ofHours(1);

    /** Hard ceiling so the map cannot grow without bound. */
    private static final int MAX_ENTRIES = 500;

    private final ConcurrentHashMap<UUID, GenerationJob> jobs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<GenerationKey, UUID> inFlight = new ConcurrentHashMap<>();
    private final Clock clock;

    /** Injected so the TTL is testable without sleeping. */
    public GenerationJobRegistry(Clock clock) {
        this.clock = clock;
    }

    private record GenerationKey(UUID schoolId, UUID academicPeriodId) {}

    /**
     * Claims the right to generate for this school and period.
     *
     * @return the new job, or empty if one is already running — the caller turns that into a 409
     *         naming {@link #runningJobId}
     */
    public Optional<GenerationJob> reserve(UUID schoolId, UUID academicPeriodId) {
        sweep();
        GenerationKey key = new GenerationKey(schoolId, academicPeriodId);
        GenerationJob job = new GenerationJob(schoolId, academicPeriodId, Instant.now(clock));

        if (inFlight.putIfAbsent(key, job.getJobId()) != null) {
            return Optional.empty();
        }
        jobs.put(job.getJobId(), job);
        return Optional.of(job);
    }

    /** The job already generating for this period, if there is one. */
    public UUID runningJobId(UUID schoolId, UUID academicPeriodId) {
        return inFlight.get(new GenerationKey(schoolId, academicPeriodId));
    }

    /** Must run in a {@code finally}, or the period stays locked until restart. */
    public void release(UUID schoolId, UUID academicPeriodId) {
        inFlight.remove(new GenerationKey(schoolId, academicPeriodId));
    }

    /** Another school's job reads as absent, not forbidden — a 403 would confirm it exists. */
    public Optional<GenerationJob> find(UUID jobId, UUID schoolId) {
        return Optional.ofNullable(jobs.get(jobId))
                .filter(job -> job.getSchoolId().equals(schoolId));
    }

    /**
     * Evicts expired jobs, lazily on submission. The map holds single digits in practice, so the
     * walk is free and no {@code @EnableScheduling} janitor thread is needed.
     */
    private void sweep() {
        Instant now = Instant.now(clock);
        jobs.values().removeIf(job ->
                job.isTerminal()
                        && job.getFinishedAt() != null
                        && job.getFinishedAt().plus(TERMINAL_TTL).isBefore(now));

        if (jobs.size() <= MAX_ENTRIES) {
            return;
        }
        List<GenerationJob> oldestTerminalFirst = jobs.values().stream()
                .filter(GenerationJob::isTerminal)
                .sorted(Comparator.comparing(GenerationJob::getSubmittedAt))
                .toList();
        for (GenerationJob job : oldestTerminalFirst) {
            if (jobs.size() <= MAX_ENTRIES) break;
            jobs.remove(job.getJobId());
        }
    }
}
