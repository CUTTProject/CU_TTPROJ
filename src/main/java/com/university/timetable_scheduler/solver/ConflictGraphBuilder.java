package com.university.timetable_scheduler.solver;

import com.university.timetable_scheduler.entity.Section;
import com.university.timetable_scheduler.repository.SectionRepository;
import com.university.timetable_scheduler.service.impl.UtilityServiceImpl;
import com.university.timetable_scheduler.status.TimetableEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import lombok.AllArgsConstructor;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the section-level conflict graph — spec §2.
 *
 * <p>An edge means "these two may not share a timeslot", for one of the spec's reasons:
 * <ul>
 *   <li>{@code SAME_LECTURER} — one person cannot teach two things at once</li>
 *   <li>{@code OVERLAPPING_STUDENTS} — a student cannot attend two things at once</li>
 * </ul>
 *
 * <p><b>Vertices are sections, not courses.</b> The spec says "Vertices V = C (one per course)" but
 * then describes edges between events, so it contradicts itself. Sections are the correct reading:
 * a course's two sections exist precisely so different student groups can take it, and forcing them
 * apart in time would be wrong. The graph is expanded from sections down to events by
 * {@link CspModelBuilder}, since events are the CSP variables.
 *
 * <p>Extracted here so the solver and the Graphviz endpoint build the graph the same way rather
 * than each keeping their own copy.
 */
@Component
@AllArgsConstructor
public class ConflictGraphBuilder {

    private final SectionRepository sectionRepository;

    /**
     * @param graph       undirected graph over section ids
     * @param conflictMap pair-key → why that edge exists (for the DOT rendering)
     * @param sectionById lookup for labelling
     */
    public record ConflictGraphResult(Graph<UUID, DefaultEdge> graph,
                                      Map<String, TimetableEnum.TimetableConflictReason> conflictMap,
                                      Map<UUID, Section> sectionById) {

        /** Flattens the graph into plain neighbour sets, which is all the solver needs. */
        public Map<UUID, java.util.Set<UUID>> toNeighbourMap() {
            Map<UUID, java.util.Set<UUID>> neighbours = new HashMap<>();
            for (UUID v : graph.vertexSet()) neighbours.put(v, new java.util.HashSet<>());
            for (DefaultEdge e : graph.edgeSet()) {
                UUID source = graph.getEdgeSource(e);
                UUID target = graph.getEdgeTarget(e);
                neighbours.computeIfAbsent(source, k -> new java.util.HashSet<>()).add(target);
                neighbours.computeIfAbsent(target, k -> new java.util.HashSet<>()).add(source);
            }
            return neighbours;
        }
    }

    public ConflictGraphResult build(List<Section> sections) {
        UUID schoolId = TenantContext.getSchoolId();
        Map<String, TimetableEnum.TimetableConflictReason> conflictMap = new HashMap<>();
        Graph<UUID, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        Map<UUID, Section> sectionById = new HashMap<>();

        for (Section section : sections) {
            graph.addVertex(section.getId());
            sectionById.put(section.getId(), section);

            addEdges(graph, conflictMap, sectionById, section,
                    sectionRepository.findIntersectingSectionByLecturer(schoolId, section.getId()),
                    TimetableEnum.TimetableConflictReason.SAME_LECTURER);

            addEdges(graph, conflictMap, sectionById, section,
                    sectionRepository.findIntersectingSectionByEnrollment(schoolId, section.getId()),
                    TimetableEnum.TimetableConflictReason.OVERLAPPING_STUDENTS);
        }
        return new ConflictGraphResult(graph, conflictMap, sectionById);
    }

    private void addEdges(Graph<UUID, DefaultEdge> graph,
                          Map<String, TimetableEnum.TimetableConflictReason> conflictMap,
                          Map<UUID, Section> sectionById,
                          Section section,
                          List<Section> intersecting,
                          TimetableEnum.TimetableConflictReason reason) {

        for (Section other : intersecting) {
            graph.addVertex(other.getId());
            sectionById.put(other.getId(), other);

            // First reason wins, so a pair sharing both a lecturer and students is labelled
            // SAME_LECTURER. The edge is what constrains the solver; the label is only for display.
            String key = UtilityServiceImpl.generateConflictKey(section.getId(), other.getId());
            if (!conflictMap.containsKey(key)) {
                conflictMap.put(key, reason);
                graph.addEdge(section.getId(), other.getId());
            }
        }
    }
}
