package com.university.timetable_scheduler.tenant;

import jakarta.servlet.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Tenant resolution is now handled by JwtAuthFilter inside the Spring Security
 * filter chain. TenantContext is populated from the JWT "schoolId" claim there.
 * This filter is kept as a no-op pass-through.
 */
@Component
@RequiredArgsConstructor
@Order(1)
public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(req, res);
    }
}

