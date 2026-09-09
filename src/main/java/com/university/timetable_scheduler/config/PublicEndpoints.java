package com.university.timetable_scheduler.config;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Arrays;
import java.util.List;

/**
 * The single source of truth for endpoints reachable without a JWT. {@link SecurityConfig} permits
 * them and {@link JwtAuthFilter} skips them; both are required, and they used to be two arrays that
 * drifted.
 *
 * <p>The filter runs before Spring Security's {@code AuthorizationFilter}, so a path added only to
 * {@code SecurityConfig} is still refused — and surfaces as a bare 403, because {@code sendError}
 * forwards to {@code /error}, which is itself authenticated.
 */
public final class PublicEndpoints {

    public static final String[] PATTERNS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/api/auth/**",
            "/api/schools/create",
            // Render polls this as its healthCheckPath and cannot present a
            // token. management.endpoint.health.show-details=never keeps the
            // response to a bare UP/DOWN.
            "/actuator/health"
    };

    private static final List<PathPattern> COMPILED =
            Arrays.stream(PATTERNS)
                    .map(PathPatternParser.defaultInstance::parse)
                    .toList();

    private PublicEndpoints() {
    }

    public static boolean isPublic(String path) {
        PathContainer container = PathContainer.parsePath(path);
        return COMPILED.stream().anyMatch(pattern -> pattern.matches(container));
    }
}
