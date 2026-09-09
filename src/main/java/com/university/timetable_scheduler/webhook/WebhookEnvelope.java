package com.university.timetable_scheduler.webhook;

import com.university.timetable_scheduler.status.WebhookEnum;

import java.time.Instant;
import java.util.UUID;

/**
 * The outer shape of every delivery, identical for all event types.
 *
 * <pre>
 * { "event": "GENERATED_TIMETABLE", "deliveryId": "8d0b...", "schoolId": "4a1c...",
 *   "timestamp": "2026-09-09T10:15:00Z", "data": { ... } }
 * </pre>
 *
 * <p>{@code deliveryId} is stable across retries of one event, so a receiver can deduplicate.
 *
 * <p>{@code timestamp} is a preformatted string, not an {@link Instant}: a public wire format must
 * not shift because {@code WRITE_DATES_AS_TIMESTAMPS} changed elsewhere.
 */
public record WebhookEnvelope(
        WebhookEnum.WebhookEventType event,
        UUID deliveryId,
        UUID schoolId,
        String timestamp,
        Object data) {

    public static WebhookEnvelope of(WebhookEnum.WebhookEventType event, UUID schoolId, Object data) {
        return new WebhookEnvelope(event, UUID.randomUUID(), schoolId, Instant.now().toString(), data);
    }
}
