package com.university.timetable_scheduler.bulk;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Parsing helpers shared by the dataset processors. Every lookup key goes through {@link #key}. */
public final class BulkValues {

    private BulkValues() {}

    /** The normalised form codes and numbers are matched on: trimmed, upper case. */
    public static String key(String raw) {
        return raw == null ? null : raw.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isBlank(String raw) {
        return raw == null || raw.isBlank();
    }

    /** Trimmed, or null when blank, so an empty cell never overwrites a stored value with "". */
    public static String text(String raw) {
        return isBlank(raw) ? null : raw.trim();
    }

    /**
     * Indexes existing rows by {@link #key} of the given field, skipping rows without one. When
     * two rows share a key the first wins, matching what the old uploads did.
     */
    public static <T> Map<String, T> index(Collection<T> items, Function<T, String> keyOf) {
        Map<String, T> index = new HashMap<>();
        for (T item : items) {
            String k = keyOf.apply(item);
            if (!isBlank(k)) {
                index.putIfAbsent(key(k), item);
            }
        }
        return index;
    }

    /** "A / B/C" → [A, B, C]. The separator the old timetable CSV used for rooms and timeslots. */
    public static List<String> splitList(String raw) {
        if (isBlank(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split("/"))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .distinct()
                .toList();
    }

    /**
     * Parses an enum cell leniently: case, surrounding spaces, and spaces or hyphens for
     * underscores are all forgiven, and a bare number matches a {@code LEVEL_} constant, so
     * "lecture theatre" and "200" both work. A blank cell is null, not an error.
     *
     * @return the constant, or null after rejecting the row when the value matches nothing
     */
    public static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, BulkRow<?> row,
                                                  String column, BulkUploadReport report) {
        if (isBlank(raw)) {
            return null;
        }
        String normalised = key(raw).replaceAll("[\\s-]+", "_");
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(normalised) || constant.name().equals("LEVEL_" + normalised)) {
                return constant;
            }
        }
        report.reject(row, column, raw, "Must be one of: " + Arrays.stream(type.getEnumConstants())
                .map(Enum::name).collect(Collectors.joining(", ")));
        return null;
    }

    /**
     * Yes/no cell. Blank is null so the caller can apply its own default.
     *
     * @return the value, or null after rejecting the row when it is not recognisable
     */
    public static Boolean parseBoolean(String raw, BulkRow<?> row, String column, BulkUploadReport report) {
        if (isBlank(raw)) {
            return null;
        }
        return switch (key(raw)) {
            case "TRUE", "YES", "Y", "1" -> true;
            case "FALSE", "NO", "N", "0" -> false;
            default -> {
                report.reject(row, column, raw, "Must be true or false");
                yield null;
            }
        };
    }
}
