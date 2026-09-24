package com.university.timetable_scheduler.webhook.payload;

import com.university.timetable_scheduler.status.BulkUploadEnum;

import java.util.UUID;

/**
 * The bulk-upload HTTP response goes to whoever uploaded; this tells the school's webhook what
 * actually landed. Sent only for committed uploads, never for a dry run.
 *
 * @param academicPeriodId set for the per-period datasets (sections, enrollments), else null
 * @param rowsProcessed    data rows received, whatever became of them
 */
public record BulkUploadResultPayload(
        UUID academicPeriodId,
        String source,
        int rowsProcessed,
        BulkUploadEnum.BulkDataset dataset,
        int created,
        int updated,
        int unchanged,
        int skipped) {

    public static final String SOURCE_CSV = "CSV";
    public static final String SOURCE_JSON = "JSON_ARRAY";
}
