package com.university.timetable_scheduler.webhook;

import com.university.timetable_scheduler.config.AsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.RejectedExecutionException;

/**
 * Serialises, signs and delivers one webhook off the calling thread, with retries.
 *
 * <p>Best-effort and not persisted: the durable copy of a timetable is on the Event rows, so a lost
 * delivery costs a notification, not the work. An outbox table would be right if the webhook were
 * the only copy — it is not.
 */
@Slf4j
@Component
public class WebhookDispatcher {

    private final WebhookProperties properties;
    private final WebhookSigner signer;
    private final WebhookUrlValidator urlValidator;
    private final ObjectMapper objectMapper;
    private final ThreadPoolTaskExecutor webhookExecutor;
    private final HttpClient httpClient;

    public WebhookDispatcher(WebhookProperties properties,
                             WebhookSigner signer,
                             WebhookUrlValidator urlValidator,
                             ObjectMapper objectMapper,
                             @Qualifier(AsyncConfig.WEBHOOK_EXECUTOR) ThreadPoolTaskExecutor webhookExecutor) {
        this.properties = properties;
        this.signer = signer;
        this.urlValidator = urlValidator;
        this.objectMapper = objectMapper;
        this.webhookExecutor = webhookExecutor;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                // Never follow redirects: a 302 to an internal address bypasses the URL check.
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** Queues a delivery. Never throws — one school's dead endpoint must not fail anyone's request. */
    public void dispatch(String url, String secret, WebhookEnvelope envelope) {
        String body;
        try {
            body = objectMapper.writeValueAsString(envelope);
        } catch (JacksonException e) {
            log.error("Could not serialise {} webhook for school {} — dropping delivery",
                    envelope.event(), envelope.schoolId(), e);
            return;
        }

        int size = body.getBytes(StandardCharsets.UTF_8).length;
        if (size > properties.getMaxPayloadBytes()) {
            // Dropped, not truncated: a receiver cannot tell a half timetable from a whole one.
            log.error("{} webhook for school {} is {} bytes, over the {} byte limit — dropping delivery",
                    envelope.event(), envelope.schoolId(), size, properties.getMaxPayloadBytes());
            return;
        }

        try {
            webhookExecutor.execute(() -> deliverWithRetries(url, secret, envelope, body));
        } catch (RejectedExecutionException e) {
            log.error("Webhook queue is full — dropped {} for school {}", envelope.event(), envelope.schoolId());
        }
    }

    /**
     * One attempt, on the calling thread, returning the status or -1. For {@code WEBHOOK_TEST}:
     * the retry loop would make a test take a minute to say "no". The response body is never
     * returned to the caller — that would make this a URL-fetching primitive.
     */
    public int deliverOnce(String url, String secret, WebhookEnvelope envelope) {
        if (!urlValidator.isSafeToSend(url)) {
            return -1;
        }
        try {
            String body = objectMapper.writeValueAsString(envelope);
            return send(url, secret, envelope, body, 1);
        } catch (JacksonException e) {
            log.error("Could not serialise {} test webhook for school {}",
                    envelope.event(), envelope.schoolId(), e);
            return -1;
        } catch (IOException e) {
            log.warn("Test delivery to school {} failed: {}", envelope.schoolId(), describe(e));
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private void deliverWithRetries(String url, String secret, WebhookEnvelope envelope, String body) {
        // Re-checked because DNS is mutable: a name safe at save time can be re-pointed since.
        if (!urlValidator.isSafeToSend(url)) {
            return;
        }

        Duration backoff = properties.getInitialBackoff();

        for (int attempt = 1; attempt <= properties.getMaxAttempts(); attempt++) {
            try {
                int status = send(url, secret, envelope, body, attempt);
                if (status >= 200 && status < 300) {
                    log.info("Delivered {} to school {} (delivery {}, attempt {}, status {})",
                            envelope.event(), envelope.schoolId(), envelope.deliveryId(), attempt, status);
                    return;
                }
                if (status >= 400 && status < 500) {
                    // The request itself is unacceptable; resending it unchanged cannot help.
                    log.warn("Receiver rejected {} for school {} with {} — not retrying",
                            envelope.event(), envelope.schoolId(), status);
                    return;
                }
                log.warn("Delivery of {} to school {} failed with status {} (attempt {}/{})",
                        envelope.event(), envelope.schoolId(), status, attempt, properties.getMaxAttempts());
            } catch (IOException e) {
                log.warn("Delivery of {} to school {} failed (attempt {}/{}): {}",
                        envelope.event(), envelope.schoolId(), attempt, properties.getMaxAttempts(),
                        describe(e));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while delivering {} to school {}", envelope.event(), envelope.schoolId());
                return;
            }

            if (attempt < properties.getMaxAttempts()) {
                // Parks a pool thread for ~10s at most — cheaper in complexity than a scheduler.
                try {
                    Thread.sleep(backoff.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                backoff = Duration.ofMillis((long) (backoff.toMillis() * properties.getBackoffMultiplier()));
            }
        }

        log.error("Gave up delivering {} to school {} after {} attempts (delivery {})",
                envelope.event(), envelope.schoolId(), properties.getMaxAttempts(), envelope.deliveryId());
    }

    /** ConnectException and friends often have a null message; then the class name is the information. */
    private static String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    private int send(String url, String secret, WebhookEnvelope envelope, String body, int attempt)
            throws IOException, InterruptedException {

        long timestamp = Instant.now().getEpochSecond();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(properties.getReadTimeout())
                .header("Content-Type", "application/json; charset=utf-8")
                .header("User-Agent", "timetable-scheduler-webhook/1")
                .header("X-Timetable-Event", envelope.event().name())
                .header("X-Timetable-Delivery", envelope.deliveryId().toString())
                .header("X-Timetable-Attempt", Integer.toString(attempt))
                .header("X-Timetable-Timestamp", Long.toString(timestamp))
                .header("X-Timetable-Signature", signer.signatureHeader(secret, timestamp, body))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        // Body discarded: we need only the status, and an unbounded read is a DoS aimed at us.
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode();
    }
}
