package com.university.timetable_scheduler.status;

/**
 * Lifecycle of an asynchronous generation job. Held in memory only, so these answer "what is
 * happening now" — the durable record of a finished run is the assignment on the Event rows.
 */
public class JobEnum {

    public enum JobStatus {
        QUEUED,

        RUNNING,

        /** Finished and committed. Does not promise a conflict-free timetable. */
        SUCCEEDED,

        FAILED,

        /** Refused before it ran because the queue was full; nothing was attempted. */
        REJECTED
    }
}
