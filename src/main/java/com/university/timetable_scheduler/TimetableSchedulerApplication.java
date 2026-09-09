package com.university.timetable_scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.university.timetable_scheduler.config.DatabaseConfigurationCheck;

@SpringBootApplication
public class TimetableSchedulerApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(TimetableSchedulerApplication.class);

        // Turns missing database environment variables into a message that names them, instead of
        // the "Unable to determine Dialect without JDBC metadata" error the datasource would
        // otherwise fail with several seconds later. See DatabaseConfigurationCheck.
        application.addListeners(new DatabaseConfigurationCheck());

        application.run(args);
    }
}
