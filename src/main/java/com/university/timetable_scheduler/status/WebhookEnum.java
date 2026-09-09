package com.university.timetable_scheduler.status;

/**
 * Event keys for outbound webhooks. Each delivery carries one, in the body and in the
 * {@code X-Timetable-Event} header.
 *
 * <p>These become a public contract as soon as a school writes a handler against them: renaming one
 * breaks every receiver, so add a key rather than repurpose one.
 */
public class WebhookEnum {

    public enum WebhookEventType {

        /** Sent only by the test endpoint, so a school can verify setup without a full solve. */
        WEBHOOK_TEST,

        /** Job accepted and the model built. Carries the problem size. */
        GENERATION_STARTED,

        /**
         * The schedule, once committed. The only success shape — a run that ran out of time still
         * reports here with {@code feasible=false}.
         */
        GENERATED_TIMETABLE,

        /**
         * Hard-constraint violations left in the generated timetable. Means the search ran out of
         * budget, unlike {@link #UNSCHEDULABLE_EVENTS}.
         */
        TIMETABLE_CONFLICTS,

        /**
         * Events no room-and-slot combination can satisfy. A data problem: excluded from the cost
         * function, so invisible in the cost, and more time would not help.
         */
        UNSCHEDULABLE_EVENTS,

        /** The structural conflict graph over sections: which pairs can never share a slot, and why. */
        CONFLICT_MAP,

        /** The run produced no timetable. */
        GENERATION_FAILED,

        /** A CSV or JSON upload committed, with the counts the HTTP response omits. */
        BULK_UPLOAD_RESULT
    }
}
