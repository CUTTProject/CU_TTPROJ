package com.university.timetable_scheduler.webhook.payload;

import java.util.UUID;

/** Carries nothing sensitive by design. */
public record WebhookTestPayload(UUID schoolId, String schoolName, String message) {
}
