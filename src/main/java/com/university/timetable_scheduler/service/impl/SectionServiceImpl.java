package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.bulk.BulkRow;
import com.university.timetable_scheduler.bulk.BulkUploadOptions;
import com.university.timetable_scheduler.bulk.BulkUploadReport;
import com.university.timetable_scheduler.bulk.BulkUploadSupport;
import com.university.timetable_scheduler.bulk.BulkValues;
import com.university.timetable_scheduler.dto.request.section.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.section.*;
import com.university.timetable_scheduler.entity.AcademicPeriod;
import com.university.timetable_scheduler.entity.Course;
import com.university.timetable_scheduler.entity.Enrollment;
import com.university.timetable_scheduler.entity.Event;
import com.university.timetable_scheduler.entity.Lecturer;
import com.university.timetable_scheduler.entity.Room;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.entity.Section;
import com.university.timetable_scheduler.entity.SectionLecturer;
import com.university.timetable_scheduler.entity.SectionRoom;
import com.university.timetable_scheduler.entity.SectionTimeslot;
import com.university.timetable_scheduler.entity.Timeslot;
import com.university.timetable_scheduler.mapper.SectionMapper;
import com.university.timetable_scheduler.repository.AcademicPeriodRepository;
import com.university.timetable_scheduler.repository.CourseRepository;
import com.university.timetable_scheduler.repository.EnrollmentRepository;
import com.university.timetable_scheduler.repository.EventRepository;
import com.university.timetable_scheduler.repository.LecturerRepository;
import com.university.timetable_scheduler.repository.RoomRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.repository.SectionLecturerRepository;
import com.university.timetable_scheduler.repository.SectionRepository;
import com.university.timetable_scheduler.repository.SectionRoomRepository;
import com.university.timetable_scheduler.repository.SectionTimeslotRepository;
import com.university.timetable_scheduler.repository.TimeslotRepository;
import com.university.timetable_scheduler.service.SectionService;
import com.university.timetable_scheduler.status.BulkUploadEnum;
import com.university.timetable_scheduler.status.EventEnum;
import com.university.timetable_scheduler.status.TimeslotEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SectionServiceImpl implements SectionService {
    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final SectionMapper sectionMapper;
    private final SchoolRepository schoolRepository;
    private final LecturerRepository lecturerRepository;
    private final RoomRepository roomRepository;
    private final TimeslotRepository timeslotRepository;
    private final SectionLecturerRepository sectionLecturerRepository;
    private final SectionRoomRepository sectionRoomRepository;
    private final SectionTimeslotRepository sectionTimeslotRepository;
    private final EventRepository eventRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final BulkUploadSupport bulkUploadSupport;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    @Override
    public CreateSectionResponse createSection(CreateSectionRequest request) {
        Course course = courseRepository.findByIdAndSchoolId(request.getSectionCourseId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        AcademicPeriod academicPeriod = academicPeriodRepository.findByIdAndSchoolId(request.getSectionAcademicPeriodId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AcademicPeriod not found"));
        Section entity = new Section();
        entity.setSchool(currentSchool());
        entity.setSectionCourse(course);
        entity.setSectionName(request.getSectionName());
        entity.setSectionEnrollmentSize(request.getSectionEnrollmentSize());
        entity.setSectionAcademicPeriod(academicPeriod);
        Section saved = sectionRepository.save(entity);
        CreateSectionResponse response = new CreateSectionResponse();
        CreateSectionResponse.Data responseData = new CreateSectionResponse.Data();
        responseData.setSection(sectionMapper.toResponse(saved));
        response.setData(responseData);
        return response;
    }

    @Override
    public ReadSectionResponse readSection(ReadSectionRequest request) {
        List<Section> list = sectionRepository.findSectionByFilter(
                TenantContext.getSchoolId(),
                request.getId(), request.getSectionCourseId(), request.getSectionName(),
                request.getSectionAcademicPeriodId(), request.getSectionStatus());
        ReadSectionResponse response = new ReadSectionResponse();
        ReadSectionResponse.Data responseData = new ReadSectionResponse.Data();
        responseData.setSections(sectionMapper.toResponseList(list));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public UpdateSectionResponse updateSection(UpdateSectionRequest request) {
        Section entity = sectionRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        if (request.getSectionName() != null) entity.setSectionName(request.getSectionName());
        if (request.getSectionEnrollmentSize() != null) entity.setSectionEnrollmentSize(request.getSectionEnrollmentSize());
        if (request.getSectionCourseId() != null) {
            Course course = courseRepository.findByIdAndSchoolId(request.getSectionCourseId(), TenantContext.getSchoolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
            entity.setSectionCourse(course);
        }
        if (request.getSectionAcademicPeriodId() != null) {
            AcademicPeriod academicPeriod = academicPeriodRepository.findByIdAndSchoolId(request.getSectionAcademicPeriodId(), TenantContext.getSchoolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AcademicPeriod not found"));
            entity.setSectionAcademicPeriod(academicPeriod);
        }
        UpdateSectionResponse response = new UpdateSectionResponse();
        UpdateSectionResponse.Data responseData = new UpdateSectionResponse.Data();
        responseData.setSection(sectionMapper.toResponse(entity));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public DeleteSectionResponse deleteSection(DeleteSectionRequest request) {
        Section entity = sectionRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        entity.setIsDeleted(true);
        return new DeleteSectionResponse();
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadSections(MultipartFile file, UUID academicPeriodId, boolean dryRun) {
        AcademicPeriod period = findPeriod(academicPeriodId);
        return bulkUploadSupport.importCsv(file, BulkUploadSectionArrayRequest.Row.class,
                new BulkUploadOptions(BulkUploadEnum.BulkDataset.SECTIONS, period.getId(), dryRun),
                (rows, report) -> processSectionRows(rows, report, period));
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadSectionsArray(BulkUploadSectionArrayRequest request, boolean dryRun) {
        AcademicPeriod period = findPeriod(request.getAcademicPeriodId());
        return bulkUploadSupport.importRows(request.getRows(),
                new BulkUploadOptions(BulkUploadEnum.BulkDataset.SECTIONS, period.getId(), dryRun),
                (rows, report) -> processSectionRows(rows, report, period));
    }

    private AcademicPeriod findPeriod(UUID academicPeriodId) {
        return academicPeriodRepository.findByIdAndSchoolId(academicPeriodId, TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Academic period not found: " + academicPeriodId));
    }

    /**
     * One row is one event. A section is identified by course code and section name within the
     * period, and the rows naming it are its complete definition: the first accepted row for a
     * section that already existed clears that section's events and its lecturer, room and
     * timeslot links, and every accepted row then adds its own. A section whose rows are all
     * rejected is left as it was.
     *
     * <p>The upload this replaced created a new section for every row, so a period may hold
     * several copies of one section. Re-uploading it keeps the oldest copy, moves the others'
     * enrollments onto it, and deletes the rest.
     */
    private void processSectionRows(List<BulkRow<BulkUploadSectionArrayRequest.Row>> rows,
                                     BulkUploadReport report, AcademicPeriod period) {
        School school = currentSchool();
        UUID schoolId = school.getId();

        Map<String, Course> coursesByCode =
                BulkValues.index(courseRepository.findAllBySchool_Id(schoolId), Course::getCourseCode);
        Map<String, Lecturer> lecturersByStaffNumber =
                BulkValues.index(lecturerRepository.findAllBySchool_Id(schoolId), Lecturer::getLecturerStaffNumber);
        Map<String, List<Room>> roomsByNumber = roomRepository.findAllBySchool_Id(schoolId).stream()
                .filter(r -> !BulkValues.isBlank(r.getRoomNumber()))
                .collect(Collectors.groupingBy(r -> BulkValues.key(r.getRoomNumber())));
        Map<String, Timeslot> timeslots = new HashMap<>();
        timeslotRepository.findAllBySchool_Id(schoolId).forEach(t -> {
            if (t.getTimeslotDay() != null && t.getTimeslotStartTime() != null && t.getTimeslotEndTime() != null) {
                timeslots.putIfAbsent(timeslotKey(t.getTimeslotDay(), t.getTimeslotStartTime(), t.getTimeslotEndTime()), t);
            }
        });
        Map<String, List<Section>> existingByKey = sectionRepository
                .findSectionByFilter(schoolId, null, null, null, period.getId(), null).stream()
                .filter(sec -> sec.getSectionCourse() != null && sec.getSectionName() != null)
                // Oldest first, so the copy kept when duplicates are merged is the original.
                .sorted(Comparator.comparing(Section::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.groupingBy(
                        sec -> BulkValues.key(sectionKey(sec.getSectionCourse().getCourseCode(), sec.getSectionName())),
                        LinkedHashMap::new, Collectors.toList()));

        Map<String, Section> sectionsByKey = new HashMap<>();
        Set<UUID> createdSections = new HashSet<>();
        Map<UUID, Set<UUID>> linkedLecturers = new HashMap<>();
        Map<UUID, Set<UUID>> linkedRooms = new HashMap<>();
        Map<UUID, Set<UUID>> linkedTimeslots = new HashMap<>();
        List<Event> events = new ArrayList<>();
        List<SectionLecturer> sectionLecturers = new ArrayList<>();
        List<SectionRoom> sectionRooms = new ArrayList<>();
        List<SectionTimeslot> sectionTimeslots = new ArrayList<>();

        for (BulkRow<BulkUploadSectionArrayRequest.Row> bulkRow : rows) {
            BulkUploadSectionArrayRequest.Row row = bulkRow.data();

            Course course = coursesByCode.get(BulkValues.key(row.getCourseCode()));
            if (course == null) {
                report.reject(bulkRow, "courseCode", row.getCourseCode(), "No course has this code. Upload courses first");
            }
            List<Lecturer> lecturers = new ArrayList<>();
            List<String> staffNumbers = BulkValues.splitList(row.getLecturerStaffNumbers());
            if (staffNumbers.isEmpty()) {
                report.reject(bulkRow, "lecturerStaffNumbers", row.getLecturerStaffNumbers(), "Give at least one staff number");
            }
            for (String staffNumber : staffNumbers) {
                Lecturer lecturer = lecturersByStaffNumber.get(BulkValues.key(staffNumber));
                if (lecturer == null) {
                    report.reject(bulkRow, "lecturerStaffNumbers", staffNumber,
                            "No lecturer has this staff number. Upload lecturers first");
                } else {
                    lecturers.add(lecturer);
                }
            }
            List<Room> rooms = new ArrayList<>();
            for (String roomNumber : BulkValues.splitList(row.getRooms())) {
                List<Room> matches = roomsByNumber.getOrDefault(BulkValues.key(roomNumber), List.of());
                if (matches.isEmpty()) {
                    report.reject(bulkRow, "rooms", roomNumber, "No room has this number. Upload rooms first");
                } else if (matches.size() > 1) {
                    report.reject(bulkRow, "rooms", roomNumber, "Several rooms have this number (buildings: "
                            + matches.stream().map(r -> String.valueOf(r.getRoomBuilding())).collect(Collectors.joining(", "))
                            + "), so it cannot say which one is meant");
                } else {
                    rooms.add(matches.get(0));
                }
            }
            List<TimeslotSpec> slotSpecs = new ArrayList<>();
            for (String entry : BulkValues.splitList(row.getTimeslot())) {
                TimeslotSpec spec = parseTimeslot(entry);
                if (spec == null) {
                    report.reject(bulkRow, "timeslot", entry,
                            "Must look like M(13:00-15:00): a day (M, T, W, TH, F, SAT, SUN) and a start before the end");
                } else {
                    slotSpecs.add(spec);
                }
            }
            EventEnum.EventType eventType = BulkValues.parseEnum(EventEnum.EventType.class,
                    row.getEventType(), bulkRow, "eventType", report);
            if (report.isRejected(bulkRow)) {
                continue;
            }

            String key = BulkValues.key(sectionKey(row.getCourseCode(), row.getSectionName()));
            Section section = sectionsByKey.get(key);
            if (section == null) {
                List<Section> existing = existingByKey.getOrDefault(key, List.of());
                if (existing.isEmpty()) {
                    section = new Section();
                    section.setSchool(school);
                    section.setSectionCourse(course);
                    section.setSectionName(row.getSectionName().trim());
                    section.setSectionAcademicPeriod(period);
                    section = sectionRepository.save(section);
                    createdSections.add(section.getId());
                } else {
                    section = existing.get(0);
                    clearSection(schoolId, section);
                    if (existing.size() > 1) {
                        mergeDuplicates(schoolId, section, existing.subList(1, existing.size()));
                        report.warn(bulkRow, "sectionName", row.getSectionName(), (existing.size() - 1)
                                + " duplicate copies of this section from earlier uploads were merged into it");
                    }
                }
                sectionsByKey.put(key, section);
            }
            if (row.getSectionEnrollmentSize() != null) {
                section.setSectionEnrollmentSize(String.valueOf(row.getSectionEnrollmentSize()));
            }

            UUID sectionId = section.getId();
            for (Lecturer lecturer : lecturers) {
                if (linkedLecturers.computeIfAbsent(sectionId, k -> new HashSet<>()).add(lecturer.getId())) {
                    SectionLecturer link = new SectionLecturer();
                    link.setSchool(school);
                    link.setSectionLecturerSection(section);
                    link.setSectionLecturerLecturer(lecturer);
                    sectionLecturers.add(link);
                }
            }
            for (Room room : rooms) {
                if (linkedRooms.computeIfAbsent(sectionId, k -> new HashSet<>()).add(room.getId())) {
                    SectionRoom link = new SectionRoom();
                    link.setSchool(school);
                    link.setSectionRoomSection(section);
                    link.setSectionRoom(room);
                    sectionRooms.add(link);
                }
            }
            for (TimeslotSpec spec : slotSpecs) {
                Timeslot timeslot = timeslots.computeIfAbsent(timeslotKey(spec.day(), spec.start(), spec.end()),
                        k -> timeslotRepository.save(newTimeslot(spec, school)));
                if (linkedTimeslots.computeIfAbsent(sectionId, k -> new HashSet<>()).add(timeslot.getId())) {
                    SectionTimeslot link = new SectionTimeslot();
                    link.setSchool(school);
                    link.setSectionTimeslotSection(section);
                    link.setSectionTimeslotTimeslot(timeslot);
                    sectionTimeslots.add(link);
                }
            }

            Event event = new Event();
            event.setSchool(school);
            event.setEventSection(section);
            event.setEventDuration(Duration.ofMinutes(row.getEventDurationMinutes()));
            event.setEventType(eventType);
            events.add(event);

            if (createdSections.contains(sectionId)) report.created(); else report.updated();
        }

        sectionLecturerRepository.saveAll(sectionLecturers);
        sectionRoomRepository.saveAll(sectionRooms);
        sectionTimeslotRepository.saveAll(sectionTimeslots);
        eventRepository.saveAll(events);
    }

    /**
     * Removes a section's events and links before its rows are re-applied. Links are deleted
     * outright: SectionRepository's lecturer-conflict query does not filter soft-deleted
     * SectionLecturer rows, so a soft-deleted link would still create clashes.
     */
    private void clearSection(UUID schoolId, Section section) {
        sectionLecturerRepository.deleteAll(
                sectionLecturerRepository.findSectionLecturerByFilter(schoolId, null, section.getId(), null));
        sectionRoomRepository.deleteAll(
                sectionRoomRepository.findAllBySectionRoomSection_Id(section.getId(), schoolId));
        sectionTimeslotRepository.deleteAll(
                sectionTimeslotRepository.findSectionTimeslotByFilter(schoolId, null, section.getId(), null));
        eventRepository.findEventByFilter(schoolId, null, section.getId(), null, null)
                .forEach(e -> e.setIsDeleted(true));
    }

    /** Moves the duplicates' enrollments onto {@code kept}, then deletes the duplicates. */
    private void mergeDuplicates(UUID schoolId, Section kept, List<Section> duplicates) {
        Set<UUID> keptStudents = new HashSet<>();
        enrollmentRepository.findEnrollmentByFilter(schoolId, null, null, kept.getId(), null)
                .forEach(e -> keptStudents.add(e.getEnrollmentStudent().getId()));
        for (Section duplicate : duplicates) {
            for (Enrollment enrollment : enrollmentRepository.findEnrollmentByFilter(schoolId, null, null, duplicate.getId(), null)) {
                if (keptStudents.add(enrollment.getEnrollmentStudent().getId())) {
                    enrollment.setEnrollmentSection(kept);
                } else {
                    enrollment.setIsDeleted(true);
                }
            }
            clearSection(schoolId, duplicate);
            duplicate.setIsDeleted(true);
        }
    }

    private static String sectionKey(String courseCode, String sectionName) {
        return courseCode.trim() + "|" + sectionName.trim();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Timeslot cells, e.g. "M(13:00-15:00)"
    // ─────────────────────────────────────────────────────────────────────────────

    private record TimeslotSpec(TimeslotEnum.TimeslotDay day, LocalTime start, LocalTime end) {}

    private static final Map<String, TimeslotEnum.TimeslotDay> DAYS = Map.ofEntries(
            Map.entry("SUN", TimeslotEnum.TimeslotDay.SUNDAY),
            Map.entry("SAT", TimeslotEnum.TimeslotDay.SATURDAY),
            Map.entry("MON", TimeslotEnum.TimeslotDay.MONDAY),
            Map.entry("TUE", TimeslotEnum.TimeslotDay.TUESDAY),
            Map.entry("WED", TimeslotEnum.TimeslotDay.WEDNESDAY),
            Map.entry("THU", TimeslotEnum.TimeslotDay.THURSDAY),
            Map.entry("FRI", TimeslotEnum.TimeslotDay.FRIDAY),
            Map.entry("TH", TimeslotEnum.TimeslotDay.THURSDAY),
            Map.entry("M", TimeslotEnum.TimeslotDay.MONDAY),
            Map.entry("T", TimeslotEnum.TimeslotDay.TUESDAY),
            Map.entry("W", TimeslotEnum.TimeslotDay.WEDNESDAY),
            Map.entry("F", TimeslotEnum.TimeslotDay.FRIDAY));

    /** @return null when the entry is malformed */
    private static TimeslotSpec parseTimeslot(String entry) {
        int open = entry.indexOf('(');
        int dash = entry.indexOf('-', open);
        int close = entry.indexOf(')', dash);
        if (open < 0 || dash < 0 || close < 0) {
            return null;
        }
        TimeslotEnum.TimeslotDay day = DAYS.get(entry.substring(0, open).trim().toUpperCase());
        if (day == null) {
            return null;
        }
        try {
            LocalTime start = LocalTime.parse(entry.substring(open + 1, dash).trim());
            LocalTime end = LocalTime.parse(entry.substring(dash + 1, close).trim());
            return start.isBefore(end) ? new TimeslotSpec(day, start, end) : null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String timeslotKey(TimeslotEnum.TimeslotDay day, LocalTime start, LocalTime end) {
        return day + "|" + start + "|" + end;
    }

    private static Timeslot newTimeslot(TimeslotSpec spec, School school) {
        Timeslot timeslot = new Timeslot();
        timeslot.setSchool(school);
        timeslot.setTimeslotDay(spec.day());
        timeslot.setTimeslotStartTime(spec.start());
        timeslot.setTimeslotEndTime(spec.end());
        timeslot.setTimeslotDuration(Duration.between(spec.start(), spec.end()));
        return timeslot;
    }
}
