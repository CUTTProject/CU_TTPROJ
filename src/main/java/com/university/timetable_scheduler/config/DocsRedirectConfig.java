package com.university.timetable_scheduler.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sends the bare server address and {@code /docs} to the documentation's front page. Spring only
 * resolves a welcome page at the root, so {@code /docs/} would otherwise 404 instead of serving
 * {@code docs/index.html}.
 */
@Configuration
public class DocsRedirectConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/docs/index.html");
        registry.addRedirectViewController("/docs", "/docs/index.html");
        registry.addRedirectViewController("/docs/", "/docs/index.html");
    }
}
