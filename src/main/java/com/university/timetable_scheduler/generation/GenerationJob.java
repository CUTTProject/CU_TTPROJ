package com.university.timetable_scheduler.generation;

import com.university.timetable_scheduler.status.JobEnum;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * The in-memory record of one generation job. Does not survive a restart — see
 * {@link GenerationJobRegistry}.
 *
 * <p>Mutable with volatile fields rather than a record: the worker updates it while a request
 * thread reads it, and the reader renders a status page, so it needs no cross-field atomicity.
 */
@Getter
public class GenerationJob {

    private final UUID jobId = UUID.randomUUID();
    private final UUID schoolId;
    private final UUID academicPeriodId;
    private final Instant submittedAt;

    private volatile JobEnum.JobStatus status = JobEnum.JobStatus.QUEUED;
    private volatile Instant startedAt;
    private volatile Instant finishedAt;

    /** Whether the caller should expect a delivery at all. */
    private volatile boolean webhookConfigured;

    private volatile Integer totalEvents;
    private volatile Integer scheduledEvents;
    private volatile Boolean feasible;
    private volatile String stopReason;

    /** Prose for a human, never an exception message. */
    private volatile String failureMessage;

    public GenerationJob(UUID schoolId, UUID academicPeriodId, Instant submittedAt) {
        this.schoolId = schoolId;
        this.academicPeriodId = academicPeriodId;
        this.submittedAt = submittedAt;
    }

    public void markRunning(Instant now) {
        this.status = JobEnum.JobStatus.RUNNING;
        this.startedAt = now;
    }

    public void markSucceeded(Instant now) {
        this.status = JobEnum.JobStatus.SUCCEEDED;
        this.finishedAt = now;
    }

    public void markFailed(Instant now, String message) {
        this.status = JobEnum.JobStatus.FAILED;
        this.finishedAt = now;
        this.failureMessage = message;
    }

    public void markRejected(Instant now) {
        this.status = JobEnum.JobStatus.REJECTED;
        this.finishedAt = now;
        this.failureMessage = "The generation queue was full; nothing was started. Retry shortly.";
    }

    public boolean isTerminal() {
        return status == JobEnum.JobStatus.SUCCEEDED
                || status == JobEnum.JobStatus.FAILED
                || status == JobEnum.JobStatus.REJECTED;
    }

    public void setWebhookConfigured(boolean webhookConfigured) { this.webhookConfigured = webhookConfigured; }
    public void setTotalEvents(Integer totalEvents) { this.totalEvents = totalEvents; }
    public void setScheduledEvents(Integer scheduledEvents) { this.scheduledEvents = scheduledEvents; }
    public void setFeasible(Boolean feasible) { this.feasible = feasible; }
    public void setStopReason(String stopReason) { this.stopReason = stopReason; }
}
