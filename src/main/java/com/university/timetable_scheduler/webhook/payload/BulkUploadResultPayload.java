package com.university.timetable_scheduler.webhook.payload;

import java.util.UUID;

/** The bulk-upload HTTP response carries only a message string; this reports what actually landed. */
public record BulkUploadResultPayload(
        UUID academicPeriodId,
        String source,
        int rowsProcessed) {

    public static final String SOURCE_CSV = "CSV";
    public static final String SOURCE_JSON = "JSON_ARRAY";
}
