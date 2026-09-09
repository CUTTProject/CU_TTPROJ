package com.university.timetable_scheduler.webhook;

import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.status.WebhookEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * The only webhook class the rest of the application touches. Callers say what happened; whether a
 * webhook exists, whether a transaction is open, signing and retries all happen behind this.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final SchoolRepository schoolRepository;
    private final WebhookDispatcher dispatcher;

    /** Snapshots the settings inside the caller's transaction, so a worker thread needs neither. */
    public WebhookTarget targetFor(UUID schoolId) {
        return schoolRepository.findLiveById(schoolId)
                .map(school -> new WebhookTarget(
                        school.getId(),
                        school.getWebhookUrl(),
                        school.getWebhookSecret(),
                        // Null reads as enabled, for rows predating the column.
                        !Boolean.FALSE.equals(school.getWebhookEnabled())))
                .orElse(null);
    }

    /** Convenience for callers that hold only a school id and are inside a transaction. */
    public void publish(UUID schoolId, WebhookEnum.WebhookEventType event, Object data) {
        publish(targetFor(schoolId), event, data);
    }

    /**
     * Queues a delivery, or does nothing if the school has no usable webhook.
     *
     * <p>Deferred until after commit when a transaction is open: the bulk-upload methods are
     * transactional, so firing immediately would announce rows the receiver cannot read yet — or
     * that roll back.
     */
    public void publish(WebhookTarget target, WebhookEnum.WebhookEventType event, Object data) {
        if (target == null || !target.isDeliverable()) {
            return;
        }

        WebhookEnvelope envelope = WebhookEnvelope.of(event, target.schoolId(), data);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            log.debug("Deferring {} for school {} until after commit", event, target.schoolId());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatcher.dispatch(target.url(), target.secret(), envelope);
                }
            });
            return;
        }

        dispatcher.dispatch(target.url(), target.secret(), envelope);
    }
}
