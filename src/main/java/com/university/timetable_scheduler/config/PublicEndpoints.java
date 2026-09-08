package com.university.timetable_scheduler.config;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Arrays;
import java.util.List;

/**
 * The single source of truth for endpoints reachable without a JWT.
 *
 * <p>Two places must agree on this list, and previously they were two separate
 * hardcoded arrays that silently drifted:
 *
 * <ul>
 *   <li>{@link SecurityConfig} permits them via {@code permitAll()}.
 *   <li>{@link JwtAuthFilter} skips them via {@code shouldNotFilter()}.
 * </ul>
 *
 * <p>Both are required. {@code JwtAuthFilter} is installed before Spring
 * Security's {@code AuthorizationFilter}, so it rejects a request with no
 * {@code Authorization} header before {@code permitAll()} is ever consulted —
 * a path added to {@code SecurityConfig} alone is still refused, and the
 * refusal surfaces as a bare 403 rather than the filter's own 401, because
 * {@code sendError} forwards to {@code /error}, which is itself authenticated.
 * That is a confusing failure to debug; keeping one list avoids it.
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
