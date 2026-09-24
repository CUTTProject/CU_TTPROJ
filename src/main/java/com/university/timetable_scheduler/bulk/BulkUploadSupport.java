package com.university.timetable_scheduler.bulk;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.service.impl.ActivityServiceImpl;
import com.university.timetable_scheduler.status.BulkUploadEnum;
import com.university.timetable_scheduler.status.WebhookEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import com.university.timetable_scheduler.webhook.WebhookService;
import com.university.timetable_scheduler.webhook.payload.BulkUploadResultPayload;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The part of every bulk upload that is not about the dataset: reading the CSV, bean-validating
 * each row, dry runs, the activity feed, the webhook, and the response.
 *
 * <p>CSV and JSON uploads share one row class per dataset. Its field names are the CSV column
 * names (matched case-insensitively), and its {@code @NotBlank}/{@code @NotNull} fields are the
 * columns a CSV must have. Rows are validated here, one at a time, rather than by {@code @Valid}
 * on the request body, so one bad row is reported instead of failing the whole request.
 *
 * <p>Callers must be {@code @Transactional}: a dry run works by marking that transaction
 * rollback-only.
 */
@Component
@AllArgsConstructor
public class BulkUploadSupport {
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ActivityServiceImpl activityService;
    private final WebhookService webhookService;

    public <R> BulkUploadResponse importCsv(MultipartFile file, Class<R> rowType,
                                            BulkUploadOptions options, BulkRowProcessor<R> processor) {
        BulkUploadReport report = new BulkUploadReport();
        List<BulkRow<R>> rows = readCsv(file, rowType, report);
        return run(rows, report, BulkUploadResultPayload.SOURCE_CSV, options, processor);
    }

    public <R> BulkUploadResponse importRows(List<R> rows, BulkUploadOptions options,
                                             BulkRowProcessor<R> processor) {
        BulkUploadReport report = new BulkUploadReport();
        List<BulkRow<R>> numbered = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i) == null) {
                report.reject(i + 1, null, null, "Row is empty");
            } else {
                numbered.add(new BulkRow<>(i + 1, rows.get(i)));
            }
        }
        report.submitted(rows.size());
        return run(numbered, report, BulkUploadResultPayload.SOURCE_JSON, options, processor);
    }

    private <R> BulkUploadResponse run(List<BulkRow<R>> rows, BulkUploadReport report, String source,
                                       BulkUploadOptions options, BulkRowProcessor<R> processor) {
        List<BulkRow<R>> valid = new ArrayList<>();
        for (BulkRow<R> row : rows) {
            Set<ConstraintViolation<R>> violations = validator.validate(row.data());
            violations.stream()
                    .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                    .forEach(v -> report.reject(row, v.getPropertyPath().toString(),
                            v.getInvalidValue(), v.getMessage()));
            if (violations.isEmpty()) {
                valid.add(row);
            }
        }

        processor.process(valid, report);

        BulkUploadEnum.BulkDataset dataset = options.dataset();
        if (options.dryRun()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } else {
            activityService.record(dataset.getActivityType(), dataset.getLabel() + " data uploaded",
                    ActivityServiceImpl.imported(report.getCreated() + report.getUpdated(), dataset.getNoun()));
            // Deferred by WebhookService until after commit.
            webhookService.publish(TenantContext.getSchoolId(),
                    WebhookEnum.WebhookEventType.BULK_UPLOAD_RESULT,
                    new BulkUploadResultPayload(options.academicPeriodId(), source, report.getSubmitted(),
                            dataset, report.getCreated(), report.getUpdated(), report.getUnchanged(),
                            report.skipped()));
        }
        return toResponse(report, options);
    }

    private static BulkUploadResponse toResponse(BulkUploadReport report, BulkUploadOptions options) {
        BulkUploadResponse.Data data = new BulkUploadResponse.Data(
                options.dataset(), options.dryRun(), report.getSubmitted(), report.getCreated(),
                report.getUpdated(), report.getUnchanged(), report.skipped(),
                report.getErrors().stream().sorted(Comparator.comparingInt(BulkUploadResponse.RowIssue::getRow)).toList(),
                report.getWarnings().stream().sorted(Comparator.comparingInt(BulkUploadResponse.RowIssue::getRow)).toList());

        String counts = "%d created, %d updated, %d unchanged, %d skipped".formatted(
                data.getCreated(), data.getUpdated(), data.getUnchanged(), data.getSkipped());
        BulkUploadResponse response = new BulkUploadResponse();
        response.setData(data);
        response.setResponseMessage(options.dryRun()
                ? "Dry run, nothing was saved. The upload would give: " + counts + "."
                : "Upload complete: " + counts + ".");
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // CSV
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Reads a CSV into row objects. File-level problems (unreadable, empty, missing required
     * columns) are a 400; a cell that cannot be converted rejects just that row.
     */
    <R> List<BulkRow<R>> readCsv(MultipartFile file, Class<R> rowType, BulkUploadReport report) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is empty");
        }
        List<Field> fields = rowFields(rowType);
        Map<String, Field> fieldsByColumn = new LinkedHashMap<>();
        fields.forEach(f -> fieldsByColumn.put(f.getName().toLowerCase(Locale.ROOT), f));
        String expected = String.join(", ", fields.stream().map(Field::getName).toList());

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] header = reader.readNext();
            if (header == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is empty");
            }

            Field[] columns = new Field[header.length];
            Set<String> present = new HashSet<>();
            for (int i = 0; i < header.length; i++) {
                // Excel prefixes UTF-8 CSVs with a byte-order mark, which would hide the first column.
                String name = header[i] == null ? "" : header[i].replace("﻿", "").trim();
                if (name.isEmpty()) {
                    continue;
                }
                Field field = fieldsByColumn.get(name.toLowerCase(Locale.ROOT));
                if (field == null) {
                    report.warn(1, name, null, "Unrecognised column, ignored. Expected columns: " + expected);
                } else if (!present.add(field.getName())) {
                    report.warn(1, name, null, "Duplicate column, only the first is read");
                } else {
                    columns[i] = field;
                }
            }

            List<String> missing = fields.stream()
                    .filter(BulkUploadSupport::isRequired)
                    .map(Field::getName)
                    .filter(name -> !present.contains(name))
                    .toList();
            if (!missing.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Missing required column(s): " + String.join(", ", missing)
                                + ". Expected columns: " + expected);
            }

            List<BulkRow<R>> rows = new ArrayList<>();
            int rowNumber = 1;
            String[] line;
            while ((line = reader.readNext()) != null) {
                rowNumber++;
                if (Arrays.stream(line).allMatch(BulkValues::isBlank)) {
                    continue;
                }
                report.submitted(1);

                Map<String, String> values = new LinkedHashMap<>();
                for (int i = 0; i < Math.min(line.length, columns.length); i++) {
                    if (columns[i] != null && !BulkValues.isBlank(line[i])) {
                        values.put(columns[i].getName(), line[i].trim());
                    }
                }
                try {
                    rows.add(new BulkRow<>(rowNumber, objectMapper.convertValue(values, rowType)));
                } catch (RuntimeException e) {
                    String column = failingColumn(e);
                    Field field = column == null ? null : fieldsByColumn.get(column.toLowerCase(Locale.ROOT));
                    report.reject(rowNumber, column, column == null ? null : values.get(column),
                            field != null && Number.class.isAssignableFrom(field.getType())
                                    ? "Must be a whole number" : "Invalid value");
                }
            }
            return rows;
        } catch (IOException | CsvException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the CSV file: " + e.getMessage());
        }
    }

    /** Every column a CSV for this row type may have, in declaration order. */
    public static List<String> columns(Class<?> rowType) {
        return rowFields(rowType).stream().map(Field::getName).toList();
    }

    /** The columns a CSV for this row type must have; a file missing one is refused. */
    public static List<String> requiredColumns(Class<?> rowType) {
        return rowFields(rowType).stream().filter(BulkUploadSupport::isRequired).map(Field::getName).toList();
    }

    private static List<Field> rowFields(Class<?> rowType) {
        return Arrays.stream(rowType.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .toList();
    }

    private static boolean isRequired(Field field) {
        return field.isAnnotationPresent(NotBlank.class)
                || field.isAnnotationPresent(NotNull.class)
                || field.isAnnotationPresent(NotEmpty.class);
    }

    /** The property Jackson failed on, from the first exception in the chain that records a path. */
    private static String failingColumn(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof JacksonException je && !je.getPath().isEmpty()) {
                return je.getPath().get(0).getPropertyName();
            }
        }
        return null;
    }
}
