package com.university.timetable_scheduler.webhook;

import java.util.UUID;

/**
 * A school's delivery settings, snapshotted while a tenant context was still available. Delivery
 * threads get one of these and never touch a repository — so they cannot read the wrong tenant.
 */
public record WebhookTarget(UUID schoolId, String url, String secret, boolean enabled) {

    /** True when this school can actually receive a delivery. */
    public boolean isDeliverable() {
        return enabled && url != null && !url.isBlank() && secret != null && !secret.isBlank();
    }
}
