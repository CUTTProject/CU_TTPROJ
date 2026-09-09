package com.university.timetable_scheduler.dto.request.school;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Configures where a school receives generated timetables.
 *
 * <p>Kept off {@code CreateSchoolRequest} and {@code UpdateSchoolRequest} on purpose:
 * {@code /api/schools/create} is public, so a URL field there is an SSRF primitive for anyone, and
 * {@code /update} maps through MapStruct with nulls ignored, which skips URL validation and makes a
 * URL impossible to clear.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ConfigureWebhookRequest {

    /** Must be https; {@code WebhookUrlValidator} does the rest, including the address check. */
    @Size(max = 2000, message = "Webhook url must not exceed 2000 characters")
    private String webhookUrl;

    /** Defaults to enabled when a url is supplied. */
    private Boolean webhookEnabled;

    /**
     * Issues a new secret, invalidating the old one. Returned once and not retrievable afterwards.
     *
     * <p>Boxed, not primitive: Jackson 3 refuses to bind an absent field into a primitive boolean,
     * which would make this field mandatory and reject every request that only set a url.
     */
    private Boolean rotateSecret;
}
