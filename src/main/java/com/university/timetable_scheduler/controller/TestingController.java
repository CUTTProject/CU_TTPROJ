package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import com.university.timetable_scheduler.service.impl.TestDataResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Development helpers. Always registered so they show in Swagger, but every call is refused with
 * a 404 unless testing.reset-enabled=true.
 */
@RestController
@RequestMapping("/api/testing")
@Tag(name = "testing-controller", description = "Development only. Disabled unless TESTING_RESET_ENABLED=true.")
public class TestingController {
    static final String CONFIRMATION = "DELETE-ALL-MY-DATA";

    private final TestDataResetService resetService;
    private final boolean resetEnabled;

    public TestingController(TestDataResetService resetService,
                             @Value("${testing.reset-enabled:false}") boolean resetEnabled) {
        this.resetService = resetService;
        this.resetEnabled = resetEnabled;
    }

    @Operation(summary = "DEVELOPMENT ONLY. Permanently deletes all of the caller's school data (departments, "
            + "lecturers, rooms, programmes, courses, curriculum, students, academic periods, sections, events, "
            + "enrollments, timeslots, activity). The school and its login are kept. Pass confirm="
            + CONFIRMATION + ". Returns 404 unless the server runs with TESTING_RESET_ENABLED=true.")
    @DeleteMapping("/reset")
    public ResetResponse reset(@RequestParam(required = false) String confirm) {
        if (!resetEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Reset is disabled on this server. Start it with TESTING_RESET_ENABLED=true to use it.");
        }
        if (!CONFIRMATION.equals(confirm)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This deletes all of your school's data. Pass confirm=" + CONFIRMATION + " to go ahead.");
        }
        ResetResponse response = new ResetResponse();
        response.setData(resetService.resetCurrentSchool());
        response.setResponseMessage("All school data deleted. The school and its login were kept.");
        return response;
    }

    @Getter
    @Setter
    public static class ResetResponse extends BaseResponse {
        /** Rows deleted per entity. */
        private Map<String, Integer> data;
    }
}
