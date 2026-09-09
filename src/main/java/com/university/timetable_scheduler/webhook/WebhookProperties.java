package com.university.timetable_scheduler.webhook;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Delivery settings, bound from {@code webhook.*}. Every value has a working default.
 *
 * <p>Config rather than constants because the right timeout depends on the receiver — a serverless
 * endpoint with a cold start needs longer than one behind a warm load balancer.
 */
@Component
@ConfigurationProperties(prefix = "webhook")
@Getter
@Setter
public class WebhookProperties {

    /** Short on purpose: an unreachable receiver is usually down, not slow. */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /** How long to wait for a response once connected. */
    private Duration readTimeout = Duration.ofSeconds(10);

    /** Attempts per delivery, including the first. Three covers ~10s of downtime. */
    private int maxAttempts = 3;

    /** Delay before the second attempt, then multiplied by {@link #backoffMultiplier}. */
    private Duration initialBackoff = Duration.ofSeconds(2);

    /** With the defaults, delays are 2s then 8s. */
    private double backoffMultiplier = 4.0;

    /**
     * Allows deliveries to loopback and private addresses. Must stay off in any deployment — the
     * URL is school-supplied, so this would expose anything reachable from inside the network,
     * including cloud metadata. For a localhost receiver in dev only.
     */
    private boolean allowPrivateHosts = false;

    /** Bodies over this are dropped, not truncated: a receiver cannot tell a half timetable from a whole one. */
    private int maxPayloadBytes = 2 * 1024 * 1024;

    /**
     * Cap on sections in a {@code CONFLICT_MAP}. The graph is dense in the worst case; the payload
     * flags truncation rather than silently looking complete.
     */
    private int maxConflictMapSections = 2000;
}
