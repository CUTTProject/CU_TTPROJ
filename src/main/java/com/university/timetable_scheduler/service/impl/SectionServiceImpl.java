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
import com.university.timetable_scheduler.status.SectionEnum;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    @Transactional
    public CreateSectionResponse createSection(CreateSectionRequest request) {
        UUID schoolId = TenantContext.getSchoolId();
        Course course = courseRepository.findByIdAndSchoolId(request.getSectionCourseId(), schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        AcademicPeriod academicPeriod = academicPeriodRepository.findByIdAndSchoolId(request.getSectionAcademicPeriodId(), schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AcademicPeriod not found"));

        SectionEnum.SectionSplit split = request.getSectionSplit() == null
                ? SectionEnum.SectionSplit.NO : request.getSectionSplit();
        Integer minutes = request.getSectionDurationMinutes();
        if (minutes == null && (request.getSectionSplit() != null || request.getRelatedToSectionId() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "sectionSplit and relatedToSectionId need sectionDurationMinutes");
        }
        if (minutes == null && request.getSectionUnits() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sectionUnits needs sectionDurationMinutes");
        }
        if (minutes != null) {
            SectionEventPlanner.Problem problem = SectionEventPlanner.problem(
                    SectionEventPlanner.unitsOf(request.getSectionUnits(), course), minutes, split);
            if (problem != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, problem.column() + ": " + problem.message());
            }
        }
        Section main = request.getRelatedToSectionId() == null ? null
                : requireMain(schoolId, request.getRelatedToSectionId(), academicPeriod, null);

        Section entity = new Section();
        entity.setSchool(currentSchool());
        entity.setSectionCourse(course);
        entity.setSectionName(request.getSectionName());
        entity.setSectionEnrollmentSize(request.getSectionEnrollmentSize());
        entity.setSectionAcademicPeriod(academicPeriod);
        if (minutes != null) {
            entity.setSectionDurationMinutes(minutes);
            entity.setSectionUnits(request.getSectionUnits());
            entity.setSectionSplit(split);
            entity.setSectionEventType(request.getSectionEventType());
        }
        entity.setSectionMergedInto(main);
        Section saved = sectionRepository.save(entity);
        eventRepository.saveAll(SectionEventPlanner.plan(saved, new Random()));
        CreateSectionResponse response = new CreateSectionResponse();
        CreateSectionResponse.Data responseData = new CreateSectionResponse.Data();
        responseData.setSection(sectionMapper.toResponse(saved));
        response.setData(responseData);
        return response;
    }

    /**
     * The section another is taught as part of, checked the way the bulk upload checks a
     * relatedTo. {@code self} is the section being related, null when it is new.
     */
    private Section requireMain(UUID schoolId, UUID mainSectionId, AcademicPeriod period, Section self) {
        Section main = sectionRepository.findByIdAndSchoolId(mainSectionId, schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Related section not found"));
        if (self != null && main.getId().equals(self.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A section cannot be related to itself");
        }
        if (main.getSectionAcademicPeriod() == null || !main.getSectionAcademicPeriod().getId().equals(period.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Section " + main.getSectionName() + " is in another academic period");
        }
        if (main.isMerged()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Section " + main.getSectionName()
                    + " is related to " + main.getSectionMergedInto().getSectionName() + "; only pairs can be related");
        }
        Section other = relatedSectionOf(schoolId, main);
        if (other != null && (self == null || !other.getId().equals(self.getId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Section " + other.getSectionName()
                    + " is already related to " + main.getSectionName() + "; only pairs can be related");
        }
        return main;
    }

    /** The section taught as part of {@code main}, if any. */
    private Section relatedSectionOf(UUID schoolId, Section main) {
        if (main.getSectionAcademicPeriod() == null) return null;
        return sectionRepository.findSectionByFilter(schoolId, null, null, null, main.getSectionAcademicPeriod().getId(), null)
                .stream()
                .filter(sec -> sec.isMerged() && sec.getSectionMergedInto().getId().equals(main.getId()))
                .findFirst().orElse(null);
    }

    /** Replaces a duration-defined section's events; one whose events were added by hand is left alone. */
    private void replanEvents(UUID schoolId, Section section, Random random) {
        if (section.getSectionDurationMinutes() == null) return;
        eventRepository.findEventByFilter(schoolId, null, section.getId(), null, null)
                .forEach(e -> e.setIsDeleted(true));
        eventRepository.saveAll(SectionEventPlanner.plan(section, random));
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
        UUID schoolId = TenantContext.getSchoolId();
        Section entity = sectionRepository.findByIdAndSchoolId(request.getId(), schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        boolean relating = request.getRelatedToSectionId() != null;
        boolean unrelating = Boolean.TRUE.equals(request.getRemoveRelatedTo());
        if (relating && unrelating) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Give relatedToSectionId or removeRelatedTo, not both");
        }
        // A pair is matched on course, name and period, so those cannot move under it.
        Section currentRelated = relatedSectionOf(schoolId, entity);
        boolean moving = request.getSectionCourseId() != null || request.getSectionName() != null
                || request.getSectionAcademicPeriodId() != null;
        if (moving && ((entity.isMerged() && !unrelating) || currentRelated != null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This section is in a related pair; "
                    + "release the pair before changing its course, name or academic period");
        }
        if (relating && currentRelated != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Section "
                    + currentRelated.getSectionName() + " is taught as part of this one; only pairs can be related");
        }
        boolean replan = request.getSectionCourseId() != null || request.getSectionDurationMinutes() != null
                || request.getSectionUnits() != null || request.getSectionSplit() != null
                || request.getSectionEventType() != null;
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
        if (request.getSectionDurationMinutes() != null) entity.setSectionDurationMinutes(request.getSectionDurationMinutes());
        if (request.getSectionUnits() != null) entity.setSectionUnits(request.getSectionUnits());
        if (request.getSectionSplit() != null) entity.setSectionSplit(request.getSectionSplit());
        if (request.getSectionEventType() != null) entity.setSectionEventType(request.getSectionEventType());

        // Checked after merging, since the duration or the course may have been left as they were.
        if (entity.getSectionDurationMinutes() == null) {
            if (request.getSectionSplit() != null || request.getSectionEventType() != null
                    || request.getSectionUnits() != null || relating) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sectionUnits, sectionSplit, "
                        + "sectionEventType and relatedToSectionId need sectionDurationMinutes");
            }
        } else {
            if (entity.getSectionSplit() == null) entity.setSectionSplit(SectionEnum.SectionSplit.NO);
            SectionEventPlanner.Problem problem = SectionEventPlanner.problem(
                    SectionEventPlanner.unitsOf(entity.getSectionUnits(), entity.getSectionCourse()),
                    entity.getSectionDurationMinutes(), entity.getSectionSplit());
            if (problem != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, problem.column() + ": " + problem.message());
            }
        }

        boolean wasMerged = entity.isMerged();
        if (relating) {
            entity.setSectionMergedInto(requireMain(schoolId, request.getRelatedToSectionId(),
                    entity.getSectionAcademicPeriod(), entity));
        }
        if (unrelating) entity.setSectionMergedInto(null);
        // A related section has no events, so pairing drops its own and releasing gives them back.
        if (replan || relating || (unrelating && wasMerged)) replanEvents(schoolId, entity, new Random());

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
     * One row is one section, identified by course code and section name within the period, and
     * is its complete definition: an accepted row for a section that already existed clears its
     * events and its lecturer, room and timeslot links, then applies its own. A rejected row
     * leaves its section as it was.
     *
     * <p>Events are planned last, from each section's duration and split, once every row's
     * {@code relatedTo} is applied: a related section gets none, and a section released from a
     * pair gets its own back.
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

        // Pre-pass, so a relatedTo can name a section further down the file and pairs are
        // checked across the whole file. Keys are of the related section (the row's) and the main
        // section (relatedTo's), whose events both are taught in.
        Map<String, Integer> firstRowByKey = new HashMap<>();
        // relatedTo is a bare section name, so it must be unique in the period across courses.
        Map<String, Set<String>> keysBySectionName = new HashMap<>();
        for (BulkRow<BulkUploadSectionArrayRequest.Row> bulkRow : rows) {
            BulkUploadSectionArrayRequest.Row row = bulkRow.data();
            String key = BulkValues.key(sectionKey(row.getCourseCode(), row.getSectionName()));
            firstRowByKey.putIfAbsent(key, bulkRow.rowNumber());
            keysBySectionName.computeIfAbsent(BulkValues.key(row.getSectionName()), k -> new LinkedHashSet<>()).add(key);
        }
        existingByKey.forEach((key, copies) -> keysBySectionName
                .computeIfAbsent(BulkValues.key(copies.get(0).getSectionName()), k -> new LinkedHashSet<>()).add(key));
        Map<String, String> mainKeyByRelatedKey = new HashMap<>();
        for (BulkRow<BulkUploadSectionArrayRequest.Row> bulkRow : rows) {
            BulkUploadSectionArrayRequest.Row row = bulkRow.data();
            String key = BulkValues.key(sectionKey(row.getCourseCode(), row.getSectionName()));
            if (firstRowByKey.get(key) != bulkRow.rowNumber() || BulkValues.isBlank(row.getRelatedTo())) continue;
            Set<String> mainKeys = keysBySectionName.getOrDefault(BulkValues.key(row.getRelatedTo()), Set.of());
            if (mainKeys.size() == 1) mainKeyByRelatedKey.put(key, mainKeys.iterator().next());
        }
        // Pairs already saved whose related section is not in this file, so they stay as they are.
        Map<String, Section> keptRelatedByMainKey = new HashMap<>();
        existingByKey.forEach((key, copies) -> {
            Section existing = copies.get(0);
            if (existing.isMerged() && !firstRowByKey.containsKey(key)) {
                keptRelatedByMainKey.put(keyOf(existing.getSectionMergedInto()), existing);
            }
        });
        Set<String> claimedMainKeys = new HashSet<>();

        Map<String, Section> sectionsByKey = new HashMap<>();
        Map<Section, String> mainKeyBySection = new LinkedHashMap<>();
        Map<Section, BulkRow<BulkUploadSectionArrayRequest.Row>> rowBySection = new HashMap<>();
        Set<UUID> createdSections = new HashSet<>();
        Map<UUID, Set<UUID>> linkedLecturers = new HashMap<>();
        Map<UUID, Set<UUID>> linkedRooms = new HashMap<>();
        Map<UUID, Set<UUID>> linkedTimeslots = new HashMap<>();
        List<SectionLecturer> sectionLecturers = new ArrayList<>();
        List<SectionRoom> sectionRooms = new ArrayList<>();
        List<SectionTimeslot> sectionTimeslots = new ArrayList<>();

        for (BulkRow<BulkUploadSectionArrayRequest.Row> bulkRow : rows) {
            BulkUploadSectionArrayRequest.Row row = bulkRow.data();
            String key = BulkValues.key(sectionKey(row.getCourseCode(), row.getSectionName()));
            int firstRow = firstRowByKey.get(key);
            if (firstRow != bulkRow.rowNumber()) {
                report.reject(bulkRow, "sectionName", row.getSectionName(),
                        "This section is already on row " + firstRow + "; give each section one row");
                continue;
            }

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
                } else if (school.overlapsBreak(spec.start(), spec.end())) {
                    // A warning would save an event restricted to a slot the solver always drops.
                    report.reject(bulkRow, "timeslot", entry, "Overlaps the school's break (%s–%s)"
                            .formatted(school.getSchoolBreakStartHour(), school.getSchoolBreakEndHour()));
                } else {
                    slotSpecs.add(spec);
                }
            }
            EventEnum.EventType eventType = BulkValues.parseEnum(EventEnum.EventType.class,
                    row.getEventType(), bulkRow, "eventType", report);
            SectionEnum.SectionSplit split = BulkValues.parseEnum(SectionEnum.SectionSplit.class,
                    row.getSplit(), bulkRow, "split", report);
            if (split == null) split = SectionEnum.SectionSplit.NO;
            if (course != null) {
                Integer units = SectionEventPlanner.unitsOf(row.getSectionUnits(), course);
                SectionEventPlanner.Problem problem =
                        SectionEventPlanner.problem(units, row.getSectionDurationMinutes(), split);
                if (problem != null) {
                    report.reject(bulkRow, problem.column(), problem.column().equals("split")
                            ? row.getSplit() : row.getSectionDurationMinutes(), problem.message());
                } else if (SectionEventPlanner.maybeNeverSplits(units, split)) {
                    report.warn(bulkRow, "split", row.getSplit(), "Only 3- and 4-unit sections split, so "
                            + "this one is always scheduled as one event");
                }
            }
            String mainKey = mainKeyByRelatedKey.get(key);
            if (!BulkValues.isBlank(row.getRelatedTo())) {
                String problem = mainKey == null
                        ? unresolvedRelatedTo(row, keysBySectionName)
                        : relationProblem(key, mainKey, existingByKey, firstRowByKey, mainKeyByRelatedKey,
                                keptRelatedByMainKey, claimedMainKeys);
                if (problem != null) report.reject(bulkRow, "relatedTo", row.getRelatedTo(), problem);
            }
            if (report.isRejected(bulkRow)) {
                continue;
            }
            if (mainKey != null) claimedMainKeys.add(mainKey);

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
            section.setSectionDurationMinutes(row.getSectionDurationMinutes());
            section.setSectionUnits(row.getSectionUnits());
            section.setSectionSplit(split);
            section.setSectionEventType(eventType);
            // The row alone decides whether this section is taught as part of another; paired below.
            section.setSectionMergedInto(null);
            mainKeyBySection.put(section, mainKey);
            rowBySection.put(section, bulkRow);

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

            if (createdSections.contains(sectionId)) report.created(); else report.updated();
        }

        sectionLecturerRepository.saveAll(sectionLecturers);
        sectionRoomRepository.saveAll(sectionRooms);
        sectionTimeslotRepository.saveAll(sectionTimeslots);

        // Only the uploaded sections' events change: a related section has none, and its main
        // section's events are the same whether or not something is taught as part of them.
        for (Map.Entry<Section, String> entry : mainKeyBySection.entrySet()) {
            if (entry.getValue() == null) continue;
            Section main = sectionsByKey.get(entry.getValue());
            if (main == null && existingByKey.containsKey(entry.getValue())) {
                main = existingByKey.get(entry.getValue()).get(0);
            }
            if (main == null) {
                // Named a section whose own row in this file was rejected.
                BulkRow<BulkUploadSectionArrayRequest.Row> relatedRow = rowBySection.get(entry.getKey());
                report.warn(relatedRow, "relatedTo", relatedRow.data().getRelatedTo(), "Ignored: the related section's "
                        + "row was rejected, so this section was saved unrelated. Fix that row and re-upload both");
                continue;
            }
            entry.getKey().setSectionMergedInto(main);
        }
        Random random = new Random();
        List<Event> events = new ArrayList<>();
        for (Section section : mainKeyBySection.keySet()) {
            if (section.getSectionDurationMinutes() == null) continue;  // events defined one by one
            if (!createdSections.contains(section.getId())) {
                eventRepository.findEventByFilter(schoolId, null, section.getId(), null, null)
                        .forEach(e -> e.setIsDeleted(true));
            }
            events.addAll(SectionEventPlanner.plan(section, random));
        }
        eventRepository.saveAll(events);
    }

    /**
     * Why a row's relatedTo cannot be applied, or null. The row's section is taught as part of the
     * main section relatedTo names. Pairs only: the main section may not itself be related to
     * another, and nothing may be related to the row's section or, besides it, to the main one.
     * A saved pair whose related section is re-uploaded in this file counts as its new row says.
     */
    private static String relationProblem(String key, String mainKey, Map<String, List<Section>> existingByKey,
                                          Map<String, Integer> firstRowByKey, Map<String, String> mainKeyByRelatedKey,
                                          Map<String, Section> keptRelatedByMainKey, Set<String> claimedMainKeys) {
        if (mainKey.equals(key)) return "A section cannot be related to itself";
        if (mainKeyByRelatedKey.containsKey(mainKey) || mainKeyByRelatedKey.containsValue(key)) {
            return "Only pairs can be related: one of these sections is related to a third in this file";
        }
        if (claimedMainKeys.contains(mainKey)) {
            return "Another row already relates to this section";
        }
        Section existingMain = existingByKey.getOrDefault(mainKey, List.of()).stream().findFirst().orElse(null);
        if (existingMain != null && existingMain.isMerged() && !firstRowByKey.containsKey(mainKey)) {
            return "That section is related to " + existingMain.getSectionMergedInto().getSectionName()
                    + "; only pairs can be related";
        }
        Section kept = keptRelatedByMainKey.get(key);
        if (kept != null) {
            return "Section " + kept.getSectionName() + " is taught as part of this one; only pairs can be related";
        }
        kept = keptRelatedByMainKey.get(mainKey);
        if (kept != null) {
            return "Section " + kept.getSectionName() + " is already related to that section; "
                    + "re-upload its row without relatedTo first";
        }
        return null;
    }

    /** Why a relatedTo names no section, or more than one, in the period. */
    private static String unresolvedRelatedTo(BulkUploadSectionArrayRequest.Row row,
                                              Map<String, Set<String>> keysBySectionName) {
        Set<String> keys = keysBySectionName.getOrDefault(BulkValues.key(row.getRelatedTo()), Set.of());
        if (keys.isEmpty()) {
            return "No section has this name in this period; give the sectionName of the section to be "
                    + "taught with, uploaded earlier or in this file";
        }
        return "Several sections have this name (courses: " + keys.stream()
                .map(k -> k.substring(0, k.indexOf('|'))).collect(Collectors.joining(", "))
                + "), so it cannot say which one is meant; give sections unique names";
    }

    private static String keyOf(Section section) {
        return BulkValues.key(sectionKey(section.getSectionCourse().getCourseCode(), section.getSectionName()));
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
        timeslot.setTimeslotSource(TimeslotEnum.TimeslotSource.CUSTOM);
        return timeslot;
    }
}
