package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.service.impl.TestDataResetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class TestingControllerTest {

    private final TestDataResetService resetService = mock(TestDataResetService.class);

    @Test
    @DisplayName("refuses with 404 when the flag is off, even with the confirmation")
    void disabledByDefault() {
        TestingController controller = new TestingController(resetService, false);

        assertThatThrownBy(() -> controller.reset(TestingController.CONFIRMATION))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(resetService);
    }

    @Test
    @DisplayName("refuses with 400 without the confirmation")
    void needsConfirmation() {
        TestingController controller = new TestingController(resetService, true);

        assertThatThrownBy(() -> controller.reset("yes"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(resetService);
    }

    @Test
    @DisplayName("resets when enabled and confirmed")
    void resets() {
        when(resetService.resetCurrentSchool()).thenReturn(Map.of("Department", 5));
        TestingController controller = new TestingController(resetService, true);

        assertThat(controller.reset(TestingController.CONFIRMATION).getData()).containsEntry("Department", 5);
    }

    /** With the flag at its default (off), the route is still in the OpenAPI document Swagger reads. */
    @Nested
    @SpringBootTest
    class ListedInSwagger {
        @Autowired private WebApplicationContext context;

        @Test
        @DisplayName("the reset route is listed in /v3/api-docs")
        void listed() throws Exception {
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
            String docs = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();

            assertThat(docs).contains("/api/testing/reset");
        }
    }
}
