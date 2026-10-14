package com.university.timetable_scheduler.entity;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Names of students and lecturers are optional, so anything showing a person goes through here. */
final class PersonNames {

    private PersonNames() {}

    /** Whichever name parts are present, joined; the identifier when there are none. */
    static String displayName(String identifier, String... nameParts) {
        String name = Stream.of(nameParts)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(" "));
        return name.isEmpty() ? identifier : name;
    }
}
