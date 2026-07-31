package com.university.timetable_scheduler.service.impl;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.opencsv.bean.CsvToBeanBuilder;
import com.university.timetable_scheduler.dto.request.timetable.*;
import com.university.timetable_scheduler.dto.response.timetable.BulkUploadTimetableResponse;
import com.university.timetable_scheduler.dto.response.timetable.TimetableEntryDTO;
import com.university.timetable_scheduler.dto.response.timetable.TimetableResponse;
import com.university.timetable_scheduler.entity.*;
import com.university.timetable_scheduler.repository.*;
import com.university.timetable_scheduler.service.TimetableService;
import com.university.timetable_scheduler.solver.*;
import com.university.timetable_scheduler.status.*;
import com.university.timetable_scheduler.tenant.TenantContext;
import lombok.AllArgsConstructor;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TimetableServiceImpl implements TimetableService {

    // static, so Lombok's @AllArgsConstructor leaves it out of the generated constructor
    private static final Logger log = LoggerFactory.getLogger(TimetableServiceImpl.class);

    private CourseRepository courseRepository;
    private DepartmentRepository departmentRepository;
    private LecturerRepository lecturerRepository;
    private SectionRepository sectionRepository;
    private SectionLecturerRepository sectionLecturerRepository;
    private SectionTimeslotRepository sectionTimeslotRepository;
    private EventRepository eventRepository;
    private TimeslotRepository timeslotRepository;
    private RoomRepository roomRepository;
    private AcademicPeriodRepository academicPeriodRepository;
    private SchoolRepository schoolRepository;
    private SectionRoomRepository sectionRoomRepository;

    private CspModelBuilder cspModelBuilder;
    private ConflictGraphBuilder conflictGraphBuilder;
    private SolverParameters solverParameters;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    /**
     * The academic-period id arrives as a String on the request DTOs, so a malformed value would
     * otherwise escape as IllegalArgumentException and surface as a 500 rather than a 400.
     */
    private static UUID parseAcademicPeriodId(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Academic period id, %s, is not a valid UUID".formatted(raw));
        }
    }

    @Override
    @Transactional
    public BulkUploadTimetableResponse bulkUploadTimetable(MultipartFile file, UUID academicPeriodId) {
        BulkUploadTimetableResponse response = new BulkUploadTimetableResponse();
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

            List<BulkUploadTimetableFileRequest> rows =
                    new CsvToBeanBuilder<BulkUploadTimetableFileRequest>(reader)
                            .withType(BulkUploadTimetableFileRequest.class)
                            .withIgnoreLeadingWhiteSpace(true)
                            .build()
                            .parse();

            processRows(rows, academicPeriodId);

            response.setError(false);
            response.setResponseCode("200");
            response.setResponseMessage("Upload complete. " + rows.size() + " row(s) processed.");

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Bulk upload failed: " + e.getMessage(), e);
        }
        return response;
    }

    @Override
    @Transactional
    public BulkUploadTimetableResponse bulkUploadTimetableArray(BulkUploadTimetableArrayRequest request) {
        List<BulkUploadTimetableFileRequest> rows = new ArrayList<>();
        for (BulkUploadTimetableArrayRequest.Row r : request.getRows()) {
            BulkUploadTimetableFileRequest row = new BulkUploadTimetableFileRequest();
            row.setCourseCode(r.getCourseCode());
            row.setCourseTitle(r.getCourseTitle());
            row.setCourseUnit(r.getCourseUnit());
            row.setCourseLevel(r.getCourseLevel());
            row.setDepartment(r.getDepartment());
            row.setLecturerStaffNumber(r.getLecturerStaffNumber());
            row.setLecturerFirstName(r.getLecturerFirstName());
            row.setLecturerLastName(r.getLecturerLastName());
            row.setLecturerEmail(r.getLecturerEmail());
            row.setSectionName(r.getSectionName());
            row.setSectionEnrollmentSize(r.getSectionEnrollmentSize());
            row.setEventDurationMinutes(r.getEventDurationMinutes());
            row.setEventType(r.getEventType());
            row.setRooms(r.getRooms());
            row.setTimeslot(r.getTimeslot());
            rows.add(row);
        }

        processRows(rows, request.getAcademicPeriodId());

        BulkUploadTimetableResponse response = new BulkUploadTimetableResponse();
        response.setError(false);
        response.setResponseCode("200");
        response.setResponseMessage("Upload complete. " + rows.size() + " row(s) processed.");
        return response;
    }

    /**
     * Core row-processing logic shared by both the CSV file upload and the array upload.
     */
    private void processRows(List<BulkUploadTimetableFileRequest> rows, UUID academicPeriodId) {
        School school = currentSchool();
        UUID schoolId = school.getId();

        AcademicPeriod period = academicPeriodRepository.findByIdAndSchoolId(academicPeriodId, schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Academic period not found: " + academicPeriodId));

        Map<String, Department> departmentCache = new HashMap<>();
        departmentRepository.findAllBySchool_Id(schoolId)
                .forEach(d -> departmentCache.put(d.getDepartmentName(), d));

        Map<String, Course> courseCache = new HashMap<>();
        courseRepository.findAllBySchool_Id(schoolId)
                .forEach(c -> courseCache.put(c.getCourseCode(), c));

        Map<String, Lecturer> lecturerCache = new HashMap<>();
        lecturerRepository.findAllBySchool_Id(schoolId)
                .forEach(l -> lecturerCache.put(l.getLecturerStaffNumber(), l));

        Map<String, Room> roomCache = new HashMap<>();
        roomRepository.findAllBySchool_Id(schoolId)
                .forEach(r -> { if (r.getRoomNumber() != null) roomCache.put(r.getRoomNumber(), r); });

        Map<String, Timeslot> timeslotCache = new HashMap<>();
        timeslotRepository.findAllBySchool_Id(schoolId)
                .forEach(t -> {
                    if (t.getTimeslotDay() != null && t.getTimeslotStartTime() != null && t.getTimeslotEndTime() != null) {
                        String key = t.getTimeslotDay() + "|" + t.getTimeslotStartTime() + "|" + t.getTimeslotEndTime();
                        timeslotCache.put(key, t);
                    }
                });

        List<Event> eventsToSave = new ArrayList<>();

        for (BulkUploadTimetableFileRequest row : rows) {
            Department dept = departmentCache.computeIfAbsent(row.getDepartment(), name -> {
                Department d = new Department();
                d.setSchool(school);
                d.setDepartmentName(name);
                return departmentRepository.save(d);
            });

            Course course = courseCache.computeIfAbsent(row.getCourseCode(), code -> {
                Course c = new Course();
                c.setSchool(school);
                c.setCourseCode(code);
                c.setCourseName(row.getCourseTitle());
                c.setCourseDescription("");
                c.setCourseUnit(row.getCourseUnit());
                c.setCourseLevel(CourseEnum.CourseLevel.valueOf(row.getCourseLevel().trim().toUpperCase()));
                return courseRepository.save(c);
            });

            Lecturer lecturer = lecturerCache.computeIfAbsent(row.getLecturerStaffNumber(), staffNum -> {
                Lecturer l = new Lecturer();
                l.setSchool(school);
                l.setLecturerStaffNumber(staffNum);
                l.setLecturerFirstName(row.getLecturerFirstName());
                l.setLecturerLastName(row.getLecturerLastName());
                l.setLecturerEmail(row.getLecturerEmail());
                l.setLecturerDepartment(dept);
                return lecturerRepository.save(l);
            });

            Section section = new Section();
            section.setSchool(school);
            section.setSectionCourse(course);
            section.setSectionName(row.getSectionName());
            section.setSectionEnrollmentSize(row.getSectionEnrollmentSize());
            section.setSectionAcademicPeriod(period);
            Section savedSection = sectionRepository.save(section);

            SectionLecturer sectionLecturer = new SectionLecturer();
            sectionLecturer.setSchool(school);
            sectionLecturer.setSectionLecturerSection(savedSection);
            sectionLecturer.setSectionLecturerLecturer(lecturer);
            sectionLecturerRepository.save(sectionLecturer);

            Event event = new Event();
            event.setSchool(school);
            event.setEventSection(savedSection);
            event.setEventDuration(Duration.ofMinutes(row.getEventDurationMinutes()));
            event.setEventType(EventEnum.EventType.valueOf(row.getEventType().trim().toUpperCase()));
            eventsToSave.add(event);

            if (row.getRooms() != null && !row.getRooms().isBlank()) {
                for (String roomName : row.getRooms().split("/")) {
                    roomName = roomName.trim();
                    if (roomName.isEmpty()) continue;
                    final String finalRoomName = roomName;
                    roomCache.computeIfAbsent(finalRoomName, rn -> {
                        List<Room> existing = roomRepository.findRoomByFilter(
                                schoolId, null, null, rn, null, null, null);
                        if (!existing.isEmpty()) return existing.get(0);
                        Room r = new Room();
                        r.setSchool(school);
                        r.setRoomNumber(rn);
                        return roomRepository.save(r);
                    });
                }
            }

            if (row.getTimeslot() != null && !row.getTimeslot().isBlank()) {
                for (String tsEntry : row.getTimeslot().split("/")) {
                    tsEntry = tsEntry.trim();
                    if (tsEntry.isEmpty()) continue;
                    Timeslot ts = parseAndUpsertTimeslot(tsEntry, timeslotCache, school);
                    if (ts != null) {
                        SectionTimeslot sectionTimeslot = new SectionTimeslot();
                        sectionTimeslot.setSchool(school);
                        sectionTimeslot.setSectionTimeslotSection(savedSection);
                        sectionTimeslot.setSectionTimeslotTimeslot(ts);
                        sectionTimeslotRepository.save(sectionTimeslot);
                    }
                }
            }
        }

        eventRepository.saveAll(eventsToSave);

    }



    /**
     * Runs the hybrid ACO + min-conflicts solver for one academic period, persists the chosen
     * timeslot/room onto each Event, and returns the assignment.
     *
     * <p>The algorithm itself lives in {@code com.university.timetable_scheduler.solver}; this
     * method is only the seam between the database and the solver. See
     * {@link com.university.timetable_scheduler.solver.AcoTimetableSolver} for the iteration loop
     * and {@link com.university.timetable_scheduler.solver.MinConflictsLocalSearch} for the spec's
     * Method 1.
     */
    @Override
    @Transactional
    public Map<UUID, EventAssignment> solve(UUID academicPeriodId) {
        UUID schoolId = TenantContext.getSchoolId();

        Optional<CspModel> model = cspModelBuilder.build(schoolId, academicPeriodId);
        if (model.isEmpty()) return Collections.emptyMap();

        SolverResult result = new AcoTimetableSolver(model.get(), solverParameters, newRandom()).solve();

        if (!result.unschedulableEventIds().isEmpty()) {
            log.warn("{} event(s) could not be scheduled by any algorithm — their duration matches "
                            + "no contiguous block of the school's timeslots. Event ids: {}",
                    result.unschedulableEventIds().size(), result.unschedulableEventIds());
        }
        if (!result.isFeasible()) {
            log.warn("No conflict-free timetable found within the {}s budget. Best found: {}. "
                            + "Returning it anyway so the clashes are visible.",
                    solverParameters.getTimeLimitSeconds(), result.cost());
        }

        persistAssignment(model.get(), result.bestSolution());
        return toAssignmentMap(model.get(), result.bestSolution());
    }

    /**
     * Seeded when {@code timetable.solver.seed} is set, so a run can be reproduced exactly;
     * otherwise fresh each time. The old solver used an unseeded Random with no way to pin it,
     * which made a disputed timetable impossible to investigate.
     */
    private Random newRandom() {
        Long seed = solverParameters.getSeed();
        return seed != null ? new Random(seed) : new Random();
    }

    /** Translates the solver's index-based solution back into entity terms for the caller. */
    private Map<UUID, EventAssignment> toAssignmentMap(CspModel model, Solution solution) {
        Map<UUID, EventAssignment> assignment = new HashMap<>();
        for (int e = 0; e < model.eventCount(); e++) {
            if (!solution.isAssigned(e)) continue;
            Candidate candidate = model.candidate(e, solution.choiceOf(e));
            assignment.put(model.event(e).getId(),
                    new EventAssignment(candidate.block().timeslots(), candidate.room()));
        }
        return assignment;
    }

    /**
     * Writes the assignment back onto the Event rows.
     *
     * <p>Only the block's <b>first</b> timeslot is stored, because {@link Event} has a single
     * {@code eventTimeslot} FK. The block's real extent is recovered by reading forward
     * {@code eventDuration} from that start — which every reader here already does. Persisting the
     * full block would need a schema change (an event↔timeslot join table); until then, treat
     * {@code eventTimeslot} as "start of block", never "the whole booking".
     */
    private void persistAssignment(CspModel model, Solution solution) {
        List<Event> toSave = new ArrayList<>();
        for (int e = 0; e < model.eventCount(); e++) {
            if (!solution.isAssigned(e)) continue;
            Candidate candidate = model.candidate(e, solution.choiceOf(e));
            Event event = model.event(e);
            event.setEventTimeslot(candidate.block().timeslots().get(0));
            event.setEventRoom(candidate.room());
            toSave.add(event);
        }
        eventRepository.saveAll(toSave);
    }


    /**
     * Runs the solver and maps the assignment to a list of {@link TimetableEntryDTO}
     * objects, sorted by day then start time. Seeds default timeslots first if the
     * school hasn't configured any, so the solver always has something to work with.
     */
    @Override
    @Transactional
    public TimetableResponse generateTimetable(GenerateTimetableRequest generateTimetableRequest) {
        UUID schoolId = TenantContext.getSchoolId();

        seedDefaultTimeslotsIfAbsent(schoolId);

        // Resolve the period before solving: the solver is now scoped to it, so an invalid id must
        // fail fast rather than after a ten-minute run.
        AcademicPeriod academicPeriod = academicPeriodRepository
                .findByIdAndSchoolId(parseAcademicPeriodId(generateTimetableRequest.getAcademicPeriodId()), schoolId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Academic period with id, %s, does not exist".formatted(generateTimetableRequest.getAcademicPeriodId())));

        // solve() also persists the chosen timeslot/room onto each Event, so the reload below picks it up.
        Map<UUID, EventAssignment> assignment = solve(academicPeriod.getId());

        List<Event> events = eventRepository.findAllBySchoolIdAndAcademicPeriodId(schoolId, academicPeriod.getId());

        Map<UUID, String> sectionLecturerName = new HashMap<>();
        sectionLecturerRepository.findAllBySchool_Id(schoolId).forEach(sl -> {
            if (sl.getSectionLecturerSection() != null && sl.getSectionLecturerLecturer() != null) {
                Lecturer lec = sl.getSectionLecturerLecturer();
                String name = trim(lec.getLecturerFirstName()) + " " + trim(lec.getLecturerLastName());
                sectionLecturerName.put(sl.getSectionLecturerSection().getId(), name.trim());
            }
        });

        List<TimetableEntryDTO> entries = new ArrayList<>();
        for (Event event : events) {
            EventAssignment a = assignment.get(event.getId());

            // Solver skips events it didn't touch this run; fall back to what's already persisted.
            if ((a == null || a.timeslots().isEmpty()) && event.getEventTimeslot() != null) {
                a = new EventAssignment(List.of(event.getEventTimeslot()), event.getEventRoom());
            }

            if (a == null || a.timeslots().isEmpty()) continue;

            TimetableEntryDTO dto = new TimetableEntryDTO();
            dto.setEventId(event.getId());
            dto.setEventType(event.getEventType() != null ? event.getEventType().name() : null);
            dto.setDurationMinutes(event.getEventDuration() != null ? event.getEventDuration().toMinutes() : 0);

            Section section = event.getEventSection();
            if (section != null) {
                dto.setSectionName(section.getSectionName());
                dto.setLecturerName(sectionLecturerName.get(section.getId()));
                Course course = section.getSectionCourse();
                if (course != null) {
                    dto.setCourseCode(course.getCourseCode());
                    dto.setCourseName(course.getCourseName());
                }
            }

            Timeslot first = a.timeslots().get(0);
            dto.setDay(first.getTimeslotDay() != null ? first.getTimeslotDay().name() : null);
            dto.setStartTime(first.getTimeslotStartTime() != null ? first.getTimeslotStartTime().toString() : null);
            dto.setEndTime(endTimeOf(event, a));

            if (a.room() != null) dto.setRoom(a.room().getRoomNumber());
            entries.add(dto);
        }

        List<String> dayOrder = Arrays.asList(
                "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
        entries.sort(Comparator
                .comparingInt((TimetableEntryDTO e) ->
                        e.getDay() != null ? dayOrder.indexOf(e.getDay()) : 99)
                .thenComparing(e -> e.getStartTime() != null ? e.getStartTime() : ""));

        TimetableResponse response = new TimetableResponse();
        response.setEntries(entries);
        response.setTotalEvents(events.size());
        response.setScheduledEvents(entries.size());
        response.setResponseMessage(
                "Timetable generated. " + entries.size() + "/" + events.size() + " events scheduled.");
        return response;
    }

    /**
     * When an event's block ends.
     *
     * <p>Prefers the block's own last slot. Falls back to reading {@code eventDuration} forward
     * from the start, which is what the fallback path above needs: it reconstructs a
     * <em>single-slot</em> block from the persisted {@code eventTimeslot} (the only slot the schema
     * stores), so trusting its last slot would report a 2-hour class as 1 hour.
     */
    private static String endTimeOf(Event event, EventAssignment assignment) {
        List<Timeslot> timeslots = assignment.timeslots();
        Timeslot first = timeslots.get(0);
        if (first.getTimeslotStartTime() == null) return null;

        if (timeslots.size() > 1) {
            Timeslot last = timeslots.get(timeslots.size() - 1);
            if (last.getTimeslotEndTime() != null) return last.getTimeslotEndTime().toString();
        }
        if (event.getEventDuration() != null && !event.getEventDuration().isZero()) {
            return first.getTimeslotStartTime().plus(event.getEventDuration()).toString();
        }
        return first.getTimeslotEndTime() != null ? first.getTimeslotEndTime().toString() : null;
    }

    /**
     * Seeds Mon–Fri, 1-hour slots across the school's configured day-start/day-end
     * hours, but only if the school hasn't defined any timeslots yet.
     */
    private void seedDefaultTimeslotsIfAbsent(UUID schoolId) {
        if (timeslotRepository.countBySchool_Id(schoolId) > 0) return;

        School school = currentSchool();
        List<Timeslot> slots = new ArrayList<>();

        TimeslotEnum.TimeslotDay[] weekdays = {
            TimeslotEnum.TimeslotDay.MONDAY,
            TimeslotEnum.TimeslotDay.TUESDAY,
            TimeslotEnum.TimeslotDay.WEDNESDAY,
            TimeslotEnum.TimeslotDay.THURSDAY,
            TimeslotEnum.TimeslotDay.FRIDAY
        };

        for (TimeslotEnum.TimeslotDay day : weekdays) {
            for (int hour = school.getSchoolDayStartHour().getHour(); hour < school.getSchoolDayEndHour().getHour(); hour++) {
                slots.add(buildTimeslot(day, hour, hour + 1, school));
            }
        }

        timeslotRepository.saveAll(slots);
    }

    private Timeslot buildTimeslot(TimeslotEnum.TimeslotDay day, int startHour, int endHour, School school) {
        Timeslot ts = new Timeslot();
        ts.setSchool(school);
        ts.setTimeslotDay(day);
        ts.setTimeslotStartTime(LocalTime.of(startHour, 0));
        ts.setTimeslotEndTime(LocalTime.of(endHour, 0));
        ts.setTimeslotDuration(Duration.ofHours(1));
        return ts;
    }

    /**
     * Reads already-persisted solver results from the DB and produces a
     * landscape A4 PDF table grouped by day of the week.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] downloadTimetablePdf(DownloadTimetableRequest downloadTimetableRequest) {
        UUID schoolId = TenantContext.getSchoolId();

        AcademicPeriod academicPeriod = academicPeriodRepository
                .findByIdAndSchoolId(parseAcademicPeriodId(downloadTimetableRequest.getAcademicPeriodId()), schoolId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Academic period with id, %s does not exist".formatted(downloadTimetableRequest.getAcademicPeriodId())));

        List<Event> events = eventRepository.findAllBySchoolIdAndAcademicPeriodId(schoolId, academicPeriod.getId()).stream()
                .filter(e -> e.getEventTimeslot() != null && e.getEventRoom() != null)
                .toList();

        Map<UUID, String> sectionLecturerName = new HashMap<>();
        sectionLecturerRepository.findAllBySchool_Id(schoolId).forEach(sl -> {
            if (sl.getSectionLecturerSection() != null && sl.getSectionLecturerLecturer() != null) {
                Lecturer lec = sl.getSectionLecturerLecturer();
                String name = trim(lec.getLecturerFirstName()) + " " + trim(lec.getLecturerLastName());
                sectionLecturerName.put(sl.getSectionLecturerSection().getId(), name.trim());
            }
        });

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter   writer = new PdfWriter(baos);
            PdfDocument pdf    = new PdfDocument(writer);
            Document    doc    = new Document(pdf, PageSize.A4.rotate());
            doc.setMargins(20, 20, 20, 20);

            doc.add(new Paragraph("University Timetable")
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(12));

            Map<String, List<Event>> byDay = events.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getEventTimeslot().getTimeslotDay().name()));

            List<String> dayOrder = Arrays.asList(
                    "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");

            for (String day : dayOrder) {
                List<Event> dayEvents = byDay.get(day);
                if (dayEvents == null || dayEvents.isEmpty()) continue;

                dayEvents.sort(Comparator.comparing(
                        e -> e.getEventTimeslot().getTimeslotStartTime()));

                doc.add(new Paragraph(day)
                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                        .setFontSize(13)
                        .setMarginTop(14)
                        .setMarginBottom(4));

                Table table = new Table(UnitValue.createPercentArray(new float[]{14, 10, 22, 10, 18, 12, 10}))
                        .useAllAvailableWidth();

                String[] headers = {"Time", "Code", "Course", "Section", "Lecturer", "Room", "Type"};
                for (String h : headers) {
                    table.addHeaderCell(new Cell()
                            .add(new Paragraph(h)
                                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                                    .setFontSize(9))
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY));
                }

                for (Event event : dayEvents) {
                    Timeslot ts      = event.getEventTimeslot();
                    Section  section = event.getEventSection();
                    Course   course  = section != null ? section.getSectionCourse() : null;

                    String timeStr;
                    if (event.getEventDuration() != null && ts.getTimeslotStartTime() != null) {
                        LocalTime end = ts.getTimeslotStartTime().plus(event.getEventDuration());
                        timeStr = ts.getTimeslotStartTime() + " – " + end;
                    } else {
                        timeStr = ts.getTimeslotStartTime() + " – " + ts.getTimeslotEndTime();
                    }

                    table.addCell(cell(timeStr));
                    table.addCell(cell(course != null ? nullSafe(course.getCourseCode()) : "-"));
                    table.addCell(cell(course != null ? nullSafe(course.getCourseName()) : "-"));
                    table.addCell(cell(section != null ? nullSafe(section.getSectionName()) : "-"));
                    table.addCell(cell(sectionLecturerName.getOrDefault(
                            section != null ? section.getId() : null, "-")));
                    table.addCell(cell(event.getEventRoom() != null
                            ? nullSafe(event.getEventRoom().getRoomNumber()) : "-"));
                    table.addCell(cell(event.getEventType() != null
                            ? event.getEventType().name() : "-"));
                }

                doc.add(table);
            }

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate timetable PDF: " + e.getMessage(), e);
        }
    }

    private static Cell cell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(8));
    }
    private static String nullSafe(String s) { return s != null ? s : "-"; }
    private static String trim(String s)     { return s != null ? s : ""; }

    // Day abbreviation → full enum name, used when parsing timeslot strings like "M(13:00-15:00)".
    private static final Map<String, String> DAY_MAP = new LinkedHashMap<>();
    static {
        DAY_MAP.put("SUN", "SUNDAY");
        DAY_MAP.put("SAT", "SATURDAY");
        DAY_MAP.put("MON", "MONDAY");
        DAY_MAP.put("TUE", "TUESDAY");
        DAY_MAP.put("WED", "WEDNESDAY");
        DAY_MAP.put("THU", "THURSDAY");
        DAY_MAP.put("FRI", "FRIDAY");
        DAY_MAP.put("TH",  "THURSDAY");
        DAY_MAP.put("M",   "MONDAY");
        DAY_MAP.put("T",   "TUESDAY");
        DAY_MAP.put("W",   "WEDNESDAY");
        DAY_MAP.put("F",   "FRIDAY");
    }

    /**
     * Parses an entry like {@code M(13:00-15:00)} or {@code TH(14:00-17:00)} into
     * a persisted {@link Timeslot}, reusing existing ones from the cache.
     */
    private Timeslot parseAndUpsertTimeslot(String entry, Map<String, Timeslot> cache, School school) {
        int parenOpen  = entry.indexOf('(');
        int dash       = entry.indexOf('-', parenOpen);
        int parenClose = entry.indexOf(')', dash);
        if (parenOpen < 0 || dash < 0 || parenClose < 0) return null;

        String dayAbbr = entry.substring(0, parenOpen).trim().toUpperCase();
        String start   = entry.substring(parenOpen + 1, dash).trim();
        String end     = entry.substring(dash + 1, parenClose).trim();

        String dayName = DAY_MAP.get(dayAbbr);
        if (dayName == null) return null;

        TimeslotEnum.TimeslotDay day;
        try { day = TimeslotEnum.TimeslotDay.valueOf(dayName); }
        catch (IllegalArgumentException e) { return null; }

        LocalTime startTime, endTime;
        try {
            startTime = LocalTime.parse(start);
            endTime   = LocalTime.parse(end);
        } catch (Exception e) { return null; }

        String cacheKey = day + "|" + startTime + "|" + endTime;
        if (cache.containsKey(cacheKey)) return cache.get(cacheKey);

        Timeslot ts = new Timeslot();
        ts.setSchool(school);
        ts.setTimeslotDay(day);
        ts.setTimeslotStartTime(startTime);
        ts.setTimeslotEndTime(endTime);
        ts.setTimeslotDuration(Duration.between(startTime, endTime));
        ts = timeslotRepository.save(ts);
        cache.put(cacheKey, ts);
        return ts;
    }


    /**
     * Generates a Graphviz DOT representation of the conflict graph.
     * Red edges = same lecturer; orange edges = overlapping students.
     * Paste the result at <a href="https://dreampuf.github.io/GraphvizOnline/">...</a>
     */
    @Override
    public String getConflictGraphDot(DownloadConflictGraphRequest downloadConflictGraphRequest) {
        UUID schoolId = TenantContext.getSchoolId();
        AcademicPeriod academicPeriod = academicPeriodRepository
                .findByIdAndSchoolId(parseAcademicPeriodId(downloadConflictGraphRequest.getAcademicPeriodId()), schoolId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST, "Academic period with id %s does not exist".formatted(downloadConflictGraphRequest.getAcademicPeriodId())));
        List<Section> sections = sectionRepository
                .findSectionByFilter(schoolId, null, null,null, academicPeriod.getId(), null);

        // Same builder the solver uses, so the picture always matches what was actually solved.
        ConflictGraphBuilder.ConflictGraphResult result = conflictGraphBuilder.build(sections);
        Graph<UUID, DefaultEdge> graph = result.graph();
        Map<String, TimetableEnum.TimetableConflictReason> conflictMap = result.conflictMap();
        Map<UUID, Section> sectionById = result.sectionById();

        StringBuilder sb = new StringBuilder();
        sb.append("graph ConflictGraph {\n");
        sb.append("  rankdir=LR;\n");
        sb.append("  node [shape=ellipse, style=filled, fillcolor=lightblue];\n\n");

        // Nodes
        for (UUID id : graph.vertexSet()) {
            Section s = sectionById.get(id);
            String label = (s != null && s.getSectionName() != null)
                    ? s.getSectionName().replace("\"", "'")
                    : id.toString();
            sb.append("  \"").append(id).append("\" [label=\"").append(label).append("\"];\n");
        }

        sb.append("\n");

        // Edges
        for (DefaultEdge edge : graph.edgeSet()) {
            UUID src = graph.getEdgeSource(edge);
            UUID tgt = graph.getEdgeTarget(edge);
            String key = UtilityServiceImpl.generateConflictKey(src, tgt);
            TimetableEnum.TimetableConflictReason reason = conflictMap.get(key);

            String color = "black";
            String label = "";
            if (reason == TimetableEnum.TimetableConflictReason.SAME_LECTURER) {
                color = "red";
                label = "SAME_LECTURER";
            } else if (reason == TimetableEnum.TimetableConflictReason.OVERLAPPING_STUDENTS) {
                color = "orange";
                label = "OVERLAPPING_STUDENTS";
            }

            sb.append("  \"").append(src).append("\" -- \"").append(tgt)
              .append("\" [label=\"").append(label).append("\", color=").append(color).append("];\n");
        }

        sb.append("}\n");
        return sb.toString();
    }
}
