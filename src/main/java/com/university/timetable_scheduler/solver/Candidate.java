package com.university.timetable_scheduler.solver;

import com.university.timetable_scheduler.entity.Room;

/**
 * One value in an event's CSP domain: the pair {@code (t, r)} from the spec, where {@code t} is a
 * {@link TimeslotBlock} and {@code r} is a {@link Room}.
 *
 * <p>The index fields are the reason this type exists rather than reusing the entities directly.
 * The solver touches candidates millions of times, so it works with dense {@code int} indices into
 * {@link CspModel}'s room and timeslot tables. That turns "is this room busy at this time?" into an
 * array lookup instead of a UUID hash + entity comparison.
 *
 * @param block        the contiguous timeslots this candidate occupies
 * @param room         the room this candidate books
 * @param roomIndex    dense index of {@code room} within {@link CspModel#rooms()}
 * @param slotIndices  dense indices of each timeslot in {@code block}, for occupancy bookkeeping
 */
public record Candidate(TimeslotBlock block, Room room, int roomIndex, int[] slotIndices) {

    @Override
    public String toString() {
        return block + " @ " + (room.getRoomNumber() != null ? room.getRoomNumber() : room.getId());
    }
}
