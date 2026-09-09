package com.university.timetable_scheduler.generation;

import com.university.timetable_scheduler.dto.response.timetable.TimetableResponse;
import com.university.timetable_scheduler.entity.AcademicPeriod;
import com.university.timetable_scheduler.service.impl.TimetableServiceImpl;
import com.university.timetable_scheduler.solver.CspModel;
import com.university.timetable_scheduler.solver.SolverParameters;
import com.university.timetable_scheduler.solver.SolverResult;
import com.university.timetable_scheduler.status.WebhookEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import com.university.timetable_scheduler.webhook.WebhookProperties;
import com.university.timetable_scheduler.webhook.WebhookService;
import com.university.timetable_scheduler.webhook.WebhookTarget;
import com.university.timetable_scheduler.webhook.payload.ConflictMapPayload;
import com.university.timetable_scheduler.webhook.payload.GeneratedTimetablePayload;
import com.university.timetable_scheduler.webhook.payload.GenerationFailedPayload;
import com.university.timetable_scheduler.webhook.payload.GenerationStartedPayload;
import com.university.timetable_scheduler.webhook.payload.TimetableConflictsPayload;
import com.university.timetable_scheduler.webhook.payload.UnschedulableEventsPayload;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import com.university.timetable_scheduler.config.AsyncConfig;

/**
 * Orchestrates one asynchronous generation.
 *
 * <p>Deliberately <b>not</b> transactional: each phase is a separate call on
 * {@code TimetableServiceImpl}, so each gets its own short transaction and the solve holds no
 * connection. Adding {@code @Transactional} here would silently undo that.
 */
@Slf4j
@Component
public class TimetableGenerationRunner {

    private final TimetableServiceImpl timetableService;
    private final GenerationJobRegistry registry;
    private final WebhookService webhookService;
    private final WebhookProperties webhookProperties;
    private final SolverParameters solverParameters;
    private final Clock clock;
    private final ThreadPoolTaskExecutor solverExecutor;

    // Explicit because Lombok does not copy @Qualifier onto constructor parameters, and there
    // are two ThreadPoolTaskExecutor beans.'
    public TimetableGenerationRunner(TimetableServiceImpl timetableService,
                                     GenerationJobRegistry registry,
                                     WebhookService webhookService,
                                     WebhookProperties webhookProperties,
                                     SolverParameters solverParameters,
                                     Clock clock,
                                     @Qualifier(AsyncConfig.SOLVER_EXECUTOR) ThreadPoolTaskExecutor solverExecutor) {
        this.timetableService = timetableService;
        this.registry = registry;
        this.webhookService = webhookService;
        this.webhookProperties = webhookProperties;
        this.solverParameters = solverParameters;
        this.clock = clock;
        this.solverExecutor = solverExecutor;
    }

    /**
     * Validates, claims and queues a generation, on the request thread.
     *
     * <p>The period is resolved here, not in the job, so an unknown id is still an immediate 400
     * rather than a 202 followed by a failure webhook.
     */
    public GenerationJob submit(String rawAcademicPeriodId) {
        UUID schoolId = TenantContext.getSchoolId();
        AcademicPeriod period = timetableService.resolvePeriod(schoolId, rawAcademicPeriodId);
        UUID periodId = period.getId();

        GenerationJob job = registry.reserve(schoolId, periodId).orElseThrow(() -> {
            UUID running = registry.runningJobId(schoolId, periodId);
            return new ResponseStatusException(HttpStatus.CONFLICT,
                    "A generation is already running for this academic period. Job id: " + running);
        });

        // Read while a tenant context and session exist; delivery threads get only this snapshot.
        WebhookTarget target = webhookService.targetFor(schoolId);
        job.setWebhookConfigured(target != null && target.isDeliverable());

        try {
            solverExecutor.execute(() -> TenantContext.runWith(schoolId, () -> run(job, target)));
        } catch (RejectedExecutionException e) {
            registry.release(schoolId, periodId);
            job.markRejected(Instant.now(clock));
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The generation queue is full. Retry shortly.");
        }
        return job;
    }

    /**
     * The job body, on a solver thread with the tenant bound by the caller. Everything is caught:
     * nothing here can reach a client, and a failed job must still release its claim.
     */
    private void run(GenerationJob job, WebhookTarget target) {
        MDC.put("jobId", job.getJobId().toString());
        MDC.put("schoolId", job.getSchoolId().toString());
        job.markRunning(Instant.now(clock));

        try {
            timetableService.prepareTimeslots(job.getSchoolId());

            Optional<CspModel> model = timetableService.buildModel(job.getSchoolId(), job.getAcademicPeriodId());
            if (model.isEmpty()) {
                // Not a system error — no data for this period. Failed because nothing was produced.
                log.warn("Nothing to solve for academic period {}", job.getAcademicPeriodId());
                job.markFailed(Instant.now(clock),
                        "No events, rooms or timeslots were found for this academic period.");
                webhookService.publish(target, WebhookEnum.WebhookEventType.GENERATION_FAILED,
                        new GenerationFailedPayload(job.getJobId(), job.getAcademicPeriodId(),
                                GenerationFailedPayload.NO_MODEL,
                                "No events, rooms or timeslots were found for this academic period."));
                return;
            }

            publishStarted(job, target, model.get());

            TimetableServiceImpl.SolveOutcome outcome = timetableService.runSolver(model.get());
            SolverResult result = outcome.result();

            timetableService.applyAssignment(job.getSchoolId(), job.getAcademicPeriodId(), outcome.assignment());

            TimetableResponse timetable =
                    timetableService.buildTimetableResponse(job.getSchoolId(), job.getAcademicPeriodId());

            job.setTotalEvents(timetable.getTotalEvents());
            job.setScheduledEvents(timetable.getScheduledEvents());
            job.setFeasible(result.isFeasible());
            job.setStopReason(result.stoppedBecause().name());
            job.markSucceeded(Instant.now(clock));

            publishResults(job, target, result, timetable);

        } catch (Exception e) {
            log.error("Generation job {} failed for school {}", job.getJobId(), job.getSchoolId(), e);
            job.markFailed(Instant.now(clock), "The generation failed unexpectedly.");
            // Generic on purpose: the exception is in the server log, and the receiver is outside
            // the trust boundary.
            webhookService.publish(target, WebhookEnum.WebhookEventType.GENERATION_FAILED,
                    new GenerationFailedPayload(job.getJobId(), job.getAcademicPeriodId(),
                            GenerationFailedPayload.ERROR, "The generation failed unexpectedly."));
        } finally {
            registry.release(job.getSchoolId(), job.getAcademicPeriodId());
            MDC.clear();
        }
    }

    private void publishStarted(GenerationJob job, WebhookTarget target, CspModel model) {
        webhookService.publish(target, WebhookEnum.WebhookEventType.GENERATION_STARTED,
                new GenerationStartedPayload(
                        job.getJobId(),
                        job.getAcademicPeriodId(),
                        solverParameters.getTimeLimitSeconds(),
                        model.eventCount(),
                        model.roomCount(),
                        model.slotCount(),
                        model.averageDomainSize(),
                        model.structurallyUnschedulableEvents().size()));
    }

    /** The timetable always; diagnostics only when they have something to say. */
    private void publishResults(GenerationJob job, WebhookTarget target,
                                SolverResult result, TimetableResponse timetable) {

        webhookService.publish(target, WebhookEnum.WebhookEventType.GENERATED_TIMETABLE,
                new GeneratedTimetablePayload(
                        job.getJobId(),
                        job.getAcademicPeriodId(),
                        result.isFeasible(),
                        result.stoppedBecause().name(),
                        result.iterations(),
                        result.elapsed() != null ? result.elapsed().toSeconds() : 0,
                        timetable.getTotalEvents(),
                        timetable.getScheduledEvents(),
                        timetable.getEntries()));

        if (!result.isFeasible() && result.breakdown() != null) {
            webhookService.publish(target, WebhookEnum.WebhookEventType.TIMETABLE_CONFLICTS,
                    new TimetableConflictsPayload(
                            job.getJobId(),
                            job.getAcademicPeriodId(),
                            result.breakdown().unassignedEvents(),
                            result.breakdown().roomClashes(),
                            result.breakdown().lecturerStudentClashes(),
                            List.copyOf(result.breakdown().conflictingEventIds())));
        }

        if (!result.unschedulableEventIds().isEmpty()) {
            webhookService.publish(target, WebhookEnum.WebhookEventType.UNSCHEDULABLE_EVENTS,
                    new UnschedulableEventsPayload(
                            job.getJobId(),
                            job.getAcademicPeriodId(),
                            result.unschedulableEventIds().size(),
                            result.unschedulableEventIds()));
        }

        try {
            ConflictMapPayload conflictMap = timetableService.buildConflictMapPayload(
                    job.getSchoolId(), job.getAcademicPeriodId(),
                    webhookProperties.getMaxConflictMapSections());
            webhookService.publish(target, WebhookEnum.WebhookEventType.CONFLICT_MAP, conflictMap);
        } catch (Exception e) {
            // The timetable is already delivered; a missing conflict map does not fail the run.
            log.warn("Could not build the conflict map for job {}", job.getJobId(), e);
        }
    }
}
