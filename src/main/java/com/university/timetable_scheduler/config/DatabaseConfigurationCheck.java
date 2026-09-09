package com.university.timetable_scheduler.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * Fails startup with a readable message when the database environment variables are missing.
 *
 * <p>Without this check the failure is genuinely hard to read. {@code spring.datasource.url} is
 * assembled from placeholders, and Spring Boot's configuration-property binder ignores
 * placeholders it cannot resolve rather than throwing — so an unset {@code DB_HOST} is passed to
 * the driver <em>literally</em>, as the six characters {@code ${DB_HOST}}. The driver then fails
 * to resolve that as a hostname, Hibernate never gets a connection to read JDBC metadata from,
 * and the last error printed is:
 *
 * <pre>Unable to determine Dialect without JDBC metadata</pre>
 *
 * which mentions neither the database nor the variable that was actually missing, and sends you
 * looking for a Hibernate dialect setting that was never the problem. (An earlier version of
 * application.properties defaulted the host to {@code localhost}, which was worse still: a
 * container with no configuration dialled itself and reported "connection refused".)
 *
 * <p>Hooked to {@link ApplicationPreparedEvent}: the environment is fully populated by then —
 * including the optional {@code local.properties} import that IDE runs rely on, so a local
 * developer who configures {@code DB_URL} there is not tripped up — while bean creation, and
 * therefore the datasource, has not started.
 *
 * <p>Registered explicitly in {@code TimetableSchedulerApplication.main}. A listener for an event
 * this early cannot be a {@code @Component}: the context that would scan for it does not exist yet.
 */
public class DatabaseConfigurationCheck implements ApplicationListener<ApplicationPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();

        List<String> missing = new ArrayList<>();

        // Either a whole JDBC URL, or the parts to build one. DB_PORT is not listed: it defaults
        // to 5432 in application.properties, which is Postgres's own convention rather than a
        // guess about the deployment.
        if (isBlank(env, "DB_URL")) {
            if (isBlank(env, "DB_HOST")) {
                missing.add("DB_HOST  (or DB_URL, a complete JDBC URL)");
            }
            if (isBlank(env, "DB_NAME")) {
                missing.add("DB_NAME  (or DB_URL, a complete JDBC URL)");
            }
        }
        // Credentials are checked for presence, not for content. An EMPTY password is a real
        // configuration: a local Postgres using trust authentication accepts one, and rejecting it
        // here would block a working local setup for the sake of a deployment mistake the database
        // itself will refuse anyway.
        if (isUnset(env, "DB_USERNAME")) {
            missing.add("DB_USERNAME");
        }
        if (isUnset(env, "DB_PASSWORD")) {
            missing.add("DB_PASSWORD");
        }

        if (missing.isEmpty()) {
            return;
        }

        throw new IllegalStateException("""
                Database configuration is missing. The application cannot start.

                Not set:
                %s

                Set them one of these ways:

                  Render      Deploy from render.yaml as a Blueprint (New -> Blueprint), which
                              injects DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD from the
                              managed Postgres. Render does NOT read render.yaml for a web
                              service created by hand -- such a service starts with none of
                              these set, which is what this message usually means.

                              Setting them by hand instead: take the database's INTERNAL
                              connection string and rewrite it for JDBC, which will not accept
                              Render's postgresql://user:pass@host/db form --
                                DB_URL=jdbc:postgresql://<internal-host>:5432/<database>
                                DB_USERNAME=<user>
                                DB_PASSWORD=<password>
                              The web service and the database must share a region, or the
                              internal hostname will not resolve.

                  Compose     docker compose up --build, after copying .env.example to .env.

                  IDE / local Put DB_URL, DB_USERNAME and DB_PASSWORD in local.properties
                              (gitignored), or export them from .env before ./mvnw spring-boot:run.
                """.formatted(indent(missing)));
    }

    /** True when the variable is unset or empty — the test for values that form part of the URL. */
    private static boolean isBlank(Environment env, String name) {
        String value = env.getProperty(name);
        return value == null || value.isBlank();
    }

    /** True only when the variable is absent entirely, leaving "" a valid configured value. */
    private static boolean isUnset(Environment env, String name) {
        return env.getProperty(name) == null;
    }

    private static String indent(List<String> names) {
        return String.join("\n", names.stream().map(name -> "  - " + name).toList());
    }
}