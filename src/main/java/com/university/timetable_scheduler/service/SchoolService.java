package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.school.*;
import com.university.timetable_scheduler.dto.response.school.*;

public interface SchoolService {

    /** Sets, updates or clears the school's outbound webhook, issuing a signing secret when needed. */
    WebhookConfigResponse configureWebhook(ConfigureWebhookRequest request);

    /** Reads the webhook settings. Never returns the secret. */
    WebhookConfigResponse readWebhookConfig();

    /** Sends a single test delivery and reports the receiver's status code. */
    WebhookTestResponse testWebhook();

    CreateSchoolResponse createSchool(CreateSchoolRequest request);
    ReadSchoolResponse readSchool(ReadSchoolRequest request);
    UpdateSchoolResponse updateSchool(UpdateSchoolRequest request);
    DeleteSchoolResponse deleteSchool(DeleteSchoolRequest request);
}
