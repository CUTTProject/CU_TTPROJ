package com.university.timetable_scheduler.bulk;

import com.university.timetable_scheduler.status.BulkUploadEnum;

import java.util.UUID;

/**
 * @param academicPeriodId only for the per-period datasets; reported on the webhook
 * @param dryRun           validate and count, then roll back
 */
public record BulkUploadOptions(BulkUploadEnum.BulkDataset dataset, UUID academicPeriodId, boolean dryRun) {

    public static BulkUploadOptions of(BulkUploadEnum.BulkDataset dataset, boolean dryRun) {
        return new BulkUploadOptions(dataset, null, dryRun);
    }
}
