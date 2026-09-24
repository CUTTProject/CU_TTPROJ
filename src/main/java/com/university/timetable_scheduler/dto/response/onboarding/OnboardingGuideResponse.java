package com.university.timetable_scheduler.dto.response.onboarding;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import com.university.timetable_scheduler.status.BulkUploadEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * The steps a school goes through to get from nothing to a generated timetable, in order, with
 * how far the caller's school has got.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OnboardingGuideResponse extends BaseResponse {
    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "OnboardingGuideResponseData")
    public static class Data {
        private int totalSteps;
        private int completedSteps;
        /** The first step not yet done; the final step once every tracked step is done. */
        private int nextStep;
        private List<Step> steps;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "OnboardingGuideStep")
    public static class Step {
        /** 1-based position. */
        private int step;
        /** Stable identifier, e.g. DEPARTMENTS, ACADEMIC_PERIOD, GENERATE_TIMETABLE. */
        private String key;
        /** The bulk upload dataset, or null for a step that is not an upload. */
        private BulkUploadEnum.BulkDataset dataset;
        private String title;
        private String description;
        private String method;
        /** Placeholders in braces, e.g. {academicPeriodId}, are for the client to fill in. */
        private String endpoint;
        /** The JSON alternative to the CSV endpoint; null when there is none. */
        private String arrayEndpoint;
        /** Columns a CSV must have. Empty for a step that is not an upload. */
        private List<String> requiredColumns;
        private List<String> optionalColumns;
        /** Keys of the steps whose data this one references. */
        private List<String> dependsOn;
        /** Live records the school has for this step; null when it is not tracked. */
        private Long count;
        /** True once the school has at least one record; null when it is not tracked. */
        private Boolean completed;
        /** True when every step in {@code dependsOn} is completed. */
        private boolean ready;
    }
}
