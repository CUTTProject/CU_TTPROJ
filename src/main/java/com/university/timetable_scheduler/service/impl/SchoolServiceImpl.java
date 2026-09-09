package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.school.*;
import com.university.timetable_scheduler.dto.response.school.*;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.mapper.SchoolMapper;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.service.SchoolService;
import com.university.timetable_scheduler.status.WebhookEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import com.university.timetable_scheduler.webhook.WebhookDispatcher;
import com.university.timetable_scheduler.webhook.WebhookEnvelope;
import com.university.timetable_scheduler.webhook.WebhookSigner;
import com.university.timetable_scheduler.webhook.WebhookTarget;
import com.university.timetable_scheduler.webhook.WebhookUrlValidator;
import com.university.timetable_scheduler.webhook.payload.WebhookTestPayload;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class SchoolServiceImpl implements SchoolService {

    private final SchoolRepository schoolRepository;
    private final SchoolMapper schoolMapper;
    private final PasswordEncoder passwordEncoder;
    private final WebhookUrlValidator webhookUrlValidator;
    private final WebhookSigner webhookSigner;
    private final WebhookDispatcher webhookDispatcher;

    @Override
    public CreateSchoolResponse createSchool(CreateSchoolRequest request) {
        School entity = new School();
        entity.setSchoolName(request.getSchoolName());
        entity.setSchoolAddress(request.getSchoolAddress());
        entity.setSchoolAdminEmail(request.getSchoolAdminEmail());
        entity.setSchoolAdminPassword(passwordEncoder.encode(request.getSchoolAdminPassword()));
        entity.setSchoolPhone(request.getSchoolPhone());
        entity.setSchoolDayStartHour(request.getSchoolDayStartHour());
        entity.setSchoolDayEndHour(request.getSchoolDayEndHour());
        School saved = schoolRepository.save(entity);
        CreateSchoolResponse response = new CreateSchoolResponse();
        CreateSchoolResponse.Data data = new CreateSchoolResponse.Data();
        data.setSchool(schoolMapper.toResponse(saved));
        response.setData(data);
        return response;
    }

    /**
     * A school may only ever address itself. The caller's id comes from the JWT, never from the
     * request body, so an id naming another tenant resolves to nothing rather than to that tenant.
     */
    private School requireOwnSchool(UUID requestedId) {
        UUID schoolId = TenantContext.getSchoolId();
        if (schoolId == null || (requestedId != null && !schoolId.equals(requestedId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found");
        }
        return schoolRepository.findLiveById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));
    }

    @Override
    public ReadSchoolResponse readSchool(ReadSchoolRequest request) {
        UUID schoolId = TenantContext.getSchoolId();
        List<School> list = (request.getId() != null && !request.getId().equals(schoolId))
                ? List.of()
                : schoolRepository.findSchoolByFilter(
                        schoolId, request.getSchoolName(), request.getSchoolStatus());
        ReadSchoolResponse response = new ReadSchoolResponse();
        ReadSchoolResponse.Data data = new ReadSchoolResponse.Data();
        data.setSchools(schoolMapper.toResponseList(list));
        response.setData(data);
        return response;
    }

    @Override
    @Transactional
    public UpdateSchoolResponse updateSchool(UpdateSchoolRequest request) {
        School entity = requireOwnSchool(request.getId());
        schoolMapper.updateDtoToEntity(request, entity);
        UpdateSchoolResponse response = new UpdateSchoolResponse();
        UpdateSchoolResponse.Data data = new UpdateSchoolResponse.Data();
        data.setSchool(schoolMapper.toResponse(entity));
        response.setData(data);
        return response;
    }

    @Override
    @Transactional
    public DeleteSchoolResponse deleteSchool(DeleteSchoolRequest request) {
        School entity = requireOwnSchool(request.getId());
        entity.setIsDeleted(true);
        return new DeleteSchoolResponse();
    }

    /**
     * Sets or clears the school's webhook, issuing a signing secret when one is needed.
     *
     * <p>A secret is minted on first configuration and on rotation, and returned in that response
     * only — no endpoint reads it back.
     *
     * <p>A blank url clears the configuration, which {@code /update} cannot do because its MapStruct
     * mapping ignores nulls.
     */
    @Override
    @Transactional
    public WebhookConfigResponse configureWebhook(ConfigureWebhookRequest request) {
        School school = requireOwnSchool(null);

        boolean clearing = request.getWebhookUrl() != null && request.getWebhookUrl().isBlank();
        if (clearing) {
            school.setWebhookUrl(null);
            school.setWebhookSecret(null);
            school.setWebhookEnabled(false);
            return webhookConfigResponse(school, null, "Webhook cleared.");
        }

        if (request.getWebhookUrl() != null) {
            webhookUrlValidator.validateForStorage(request.getWebhookUrl());
            school.setWebhookUrl(request.getWebhookUrl());
        }
        if (request.getWebhookEnabled() != null) {
            school.setWebhookEnabled(request.getWebhookEnabled());
        } else if (school.getWebhookEnabled() == null) {
            school.setWebhookEnabled(true);
        }

        String issuedSecret = null;
        if (Boolean.TRUE.equals(request.getRotateSecret()) || school.getWebhookSecret() == null) {
            issuedSecret = webhookSigner.generateSecret();
            school.setWebhookSecret(issuedSecret);
        }

        String note = issuedSecret != null
                ? "Store this secret now. It is shown once and cannot be read back; rotate to get a new one."
                : "Webhook updated. The existing secret is unchanged.";
        return webhookConfigResponse(school, issuedSecret, note);
    }

    @Override
    @Transactional
    public WebhookConfigResponse readWebhookConfig() {
        return webhookConfigResponse(requireOwnSchool(null), null, null);
    }

    /**
     * Sends a {@code WEBHOOK_TEST} and reports what the receiver answered.
     *
     * <p>Synchronous and single-attempt, unlike every other delivery: the retry loop would make a
     * test take most of a minute to report a failure the caller already suspects.
     */
    @Override
    @Transactional
    public WebhookTestResponse testWebhook() {
        School school = requireOwnSchool(null);
        WebhookTarget target = new WebhookTarget(school.getId(), school.getWebhookUrl(),
                school.getWebhookSecret(), !Boolean.FALSE.equals(school.getWebhookEnabled()));

        WebhookTestResponse.Data data = new WebhookTestResponse.Data();
        if (!target.isDeliverable()) {
            data.setDelivered(false);
            data.setMessage("No webhook is configured, or it is disabled.");
            return wrapTest(data);
        }

        WebhookEnvelope envelope = WebhookEnvelope.of(
                WebhookEnum.WebhookEventType.WEBHOOK_TEST, school.getId(),
                new WebhookTestPayload(school.getId(), school.getSchoolName(),
                        "This is a test delivery. If you can verify its signature, you are set up correctly."));

        int status = webhookDispatcher.deliverOnce(target.url(), target.secret(), envelope);
        data.setStatusCode(status > 0 ? status : null);
        data.setDelivered(status >= 200 && status < 300);
        data.setMessage(switch (status / 100) {
            case 2 -> "Your endpoint accepted the delivery.";
            case 4 -> "Your endpoint rejected the delivery. Check the path and your signature check.";
            case 5 -> "Your endpoint returned a server error.";
            default -> "The delivery could not be completed. Check that the url is reachable and https.";
        });
        return wrapTest(data);
    }

    private WebhookTestResponse wrapTest(WebhookTestResponse.Data data) {
        WebhookTestResponse response = new WebhookTestResponse();
        response.setData(data);
        return response;
    }

    private WebhookConfigResponse webhookConfigResponse(School school, String issuedSecret, String note) {
        WebhookConfigResponse.Data data = new WebhookConfigResponse.Data();
        data.setWebhookUrl(school.getWebhookUrl());
        data.setWebhookEnabled(!Boolean.FALSE.equals(school.getWebhookEnabled()) && school.getWebhookUrl() != null);
        data.setSecretConfigured(school.getWebhookSecret() != null);
        data.setWebhookSecret(issuedSecret);
        data.setNote(note);

        WebhookConfigResponse response = new WebhookConfigResponse();
        response.setData(data);
        return response;
    }
}
