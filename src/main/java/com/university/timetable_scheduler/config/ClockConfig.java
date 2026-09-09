package com.university.timetable_scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * A {@link Clock} bean, so time-dependent components are testable without sleeping. UTC because its
 * values reach webhook payloads and job timestamps, which cross machines.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
