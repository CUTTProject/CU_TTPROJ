# Timetable Scheduler — Algorithm Documentation

The solver assigns every **Event** a **Room** and a contiguous **block of Timeslots** such that no
hard constraint is violated.

It is a **MAX-MIN Ant System (ACO) with a min-conflicts local-search daemon** — a hybrid that
implements both halves of the spec:

| Requirement | Where it lives |
|---|---|
| Ant Colony Optimization with pheromones | `PheromoneMatrix`, `AntSolutionBuilder`, `AcoTimetableSolver` |
| `Algorithm - Stage 1.pdf` §1–3 — CSP model & conflict graph | `CspModelBuilder`, `ConflictGraphBuilder`, `CspModel` |
| `Algorithm - Stage 1.pdf` p.3 "Method 1: Local Search" | `MinConflictsLocalSearch` |
| `Algorithm - Stage 1.pdf` p.4 Note 1 — `CONFLICTS(csp, var, v, current)` | `ConflictCounter` |

Hybridising ACO with local search is standard for university timetabling: pure constructive ACO
underperforms, and the local search is where most of the measurable gain comes from.

> Background on why the previous implementation was replaced:
> [`docs/TIMETABLE_ALGORITHM_REVIEW.md`](docs/TIMETABLE_ALGORITHM_REVIEW.md).

---

## Package layout

Everything lives in `com.university.timetable_scheduler.solver`. `TimetableServiceImpl` only wires
the database to the solver; it contains no algorithm.

```
Value types      TimeslotBlock ── a contiguous run of slots on one day
                 Candidate ───── one domain value: (block, room) + dense indices
                 Solution ────── int[] : event index → chosen candidate index
                 SolutionCost ── weighted objective
                 Deadline ────── the spec's Time_Limit

Model            ConflictGraphBuilder → section conflict graph (spec §2)
                 CspModelBuilder ───── DB → CspModel (spec §1, §3)
                 CspModel ──────────── the immutable (X, D, Constraints) triple

Machinery        ConflictCounter ───── CONFLICTS(...) + occupancy index   ← the core
                 IndexedEventSet ───── O(1) random pick of a conflicted event
                 PheromoneMatrix ───── τ: evaporate / deposit / MMAS bounds

Search           AntSolutionBuilder ── probabilistic construction (τ^α · η^β)
                 MinConflictsLocalSearch → the spec's Method 1 (the daemon)
                 AcoTimetableSolver ── the iteration loop
                 SolverResult ─────── best solution + why it stopped
```

---

## 1. The CSP model (spec §1–§3)

`CspModelBuilder.build(schoolId, academicPeriodId)` turns the database into a `CspModel`.

- **Variables (X)** — one per event, densely indexed `0..n-1`.
- **Domains (D)** — every legal `(timeslotBlock, room)` pair, precomputed as the spec requires
  ("Di must be precomputed for each ei to reduce the search space").
- **Constraints** — two hard rules:
  1. **Colouring** — conflict-graph neighbours may not overlap in time (shared lecturer or students).
  2. **Room exclusivity** — no two events may hold the same room at overlapping times, neighbours
     or not.

Everything is `int`-indexed, so a whole timetable is one `int[]` and τ is a `double[][]`. That is
what makes hundreds of thousands of ant constructions affordable inside a 10-minute budget.

### Deliberate deviations from the spec

| Spec says | We do | Why |
|---|---|---|
| "Vertices V = C (one per course)" | Vertices are **sections** | The spec contradicts itself (it then describes edges between *events*). A course's two sections exist so different cohorts can take it; forcing them apart in time would be wrong. |
| Colouring is `ti ≠ tj` | Interval **overlap** | Necessary once events span multiple slots. `ti ≠ tj` would let a 9–11 block coexist with a 10–11 block. |
| §3 "Room capacity sufficient" | **Not enforced** | Deliberately out of scope while the data is auto-generated. See below. |
| §1.iii — split a 3-unit course into a 2h + a 1h event | Durations come from the **CSV** | Per stakeholder decision; the upload supplies one event per row. |

### Room capacity

Not implemented, by decision. To enable it, filter by capacity in `CspModelBuilder.buildDomains`
(there is a marked comment at the exact spot) — the domain shrinks and nothing else changes.
Note `Section.sectionEnrollmentSize` is a `String` and would need parsing.

### Structurally unschedulable events

An event whose duration matches no contiguous block (e.g. 90 minutes against 1-hour slots) has an
**empty domain**. No algorithm can place it. Such events are excluded from the search and reported
via `SolverResult.unschedulableEventIds()`, rather than silently retried forever. This is almost
always a data problem.

---

## 2. `ConflictCounter` — the core

Implements the spec's `CONFLICTS(csp, var, v, current)` (p.4, Note 1):

```java
int conflictsIfAssigned(int event, int candidate)
```

> "How many hard-constraint violations would result from putting this event here?"

Everything else derives from it — the ants' heuristic η, min-conflicts' argmin, and the cost. It is
the single most important class in the package.

**Why it is fast.** Two indices reduce the check to a candidate's own footprint rather than a scan
of every event:
- **Room exclusivity** — a `(room, slot) → events` occupancy table.
- **Colouring** — only the event's conflict-graph neighbours are examined.

It is **stateful**: it owns a `Solution` and maintains the occupancy index and per-event conflict
counts incrementally. Always mutate through `assign()` / `unassign()`, never the `Solution`
directly, or the indices drift out of sync.

### Cost

```
cost = 100 × unassignedEvents  +  10 × conflictingPairs
```

An unplaced event must dominate any number of clashes, hence the weights. Room clashes and
lecturer/student clashes are *equally* severe, so the search only ever needs the **number of
distinct conflicting pairs** — not their reasons. A pair violating both rules at once counts once.
The human-facing split is recovered separately, once, by `breakdown()`.

---

## 3. `MinConflictsLocalSearch` — the spec's Method 1

The page-3 flowchart, line for line:

| Flowchart | Code |
|---|---|
| Input: CSP, Time_Limit | the model, plus `Deadline` |
| Current :- complete assignment | the ant's constructed solution |
| Time_Limit reached? | `deadline.isExpired()` |
| Is Current the CSP solution? | `!counter.hasConflicts()` |
| Var :- a randomly chosen variable | `counter.randomConflictedEvent(random)` |
| Value :- a value v (Note 1) | `argMinConflicts(...)` |
| Set Var = Value | `counter.assign(var, value)` |

Two additions beyond the literal flowchart, both standard:
- **Random walk moves** (`localSearchWalkProbability`) — pure min-conflicts stalls on plateaux where
  no single move improves anything.
- **Best-ever tracking** — because walk moves and ties can worsen the state, the search restores its
  best state at the end.

Ties in the argmin are broken **at random**. With uniform slots and interchangeable rooms, ties are
the common case; deterministic tie-breaking makes the search revisit states and stall.

---

## 4. The ACO layer

### `AntSolutionBuilder` — construction

Ants visit events **most-constrained-first** (fewest candidates, then highest conflict-graph degree
— the CSP *MRV* heuristic) and pick each placement probabilistically:

```
              τ[e][k]^α · η(e,k)^β
  P(e,k) = ─────────────────────────────         η(e,k) = 1 / (1 + CONFLICTS(e, k, current))
             Σ_j  τ[e][j]^α · η(e,j)^β
```

- **α** — trust in what the colony learned.
- **β** — trust in the conflict count in front of the ant. Held **above α** on purpose: "don't clash
  right now" is a strong signal for timetabling; pheromone should refine that, not overrule it.
- **q0** — chance of exploiting the best candidate outright (ACS pseudo-random-proportional rule).
- **candidateSampleSize** — domains run to the hundreds, so each ant weighs a random subset. Set 0
  to consider whole domains (exact, much slower). τ still spans the full domain.

Ants build **from empty** rather than perturbing an incumbent — τ is only informative if it gets to
shape a whole solution.

### `PheromoneMatrix` — the colony's memory

`τ[e][k]` = learned desirability of giving event `e` candidate `k`. **MAX-MIN Ant System**:

1. **Only the best ant deposits** — `τ ← τ + Q/(1+cost)`. Letting every ant deposit averages the
   signal into mush.
2. **τ is clamped to `[τmin, τmax]`** — the safeguard that makes (1) survivable. Without a floor, a
   candidate unused early decays to ~0, leaves the roulette wheel for good, and the colony converges
   prematurely.
   ```
   τmax = 1 / (ρ · (1 + cost_best))          τmin = τmax / (2 · avgDomainSize)
   ```
3. **τ starts at τmax** — optimistic, so early iterations explore broadly.
4. **Evaporation** — `τ ← (1−ρ)·τ` every iteration. Without it, early accidents accumulate forever.

### `AcoTimetableSolver` — the loop

```
for each ant:
    construct a timetable      (explore)
    polish with min-conflicts  (exploit)
    keep the iteration's best
update the global best         (elitism)
evaporate τ, then let the global best deposit
if stagnating for N iterations: flatten τ (global best is kept)
```

Stops at the first of **cost 0** (provably feasible) or **Time_Limit**, and always returns the
**best solution ever seen** — never merely the last one tried.

Single-threaded by design: deterministic under a fixed seed, which is worth more for debugging and
demos than the speedup would be.

---

## 5. Configuration

All under `timetable.solver.*` in `application.properties`; defaults live in `SolverParameters`.

| Property | Default | Meaning |
|---|---|---|
| `time-limit-seconds` | `600` | The spec's Time_Limit |
| `ants` | `10` | Ants per iteration |
| `alpha` | `1.0` | Pheromone weight |
| `beta` | `3.0` | Heuristic weight |
| `evaporation-rate` | `0.05` | ρ |
| `deposit-constant` | `1.0` | Q |
| `greedy-selection-probability` | `0.1` | q0 (ACS rule) |
| `candidate-sample-size` | `64` | Candidates weighed per event; 0 = whole domain |
| `local-search-max-steps` | `20000` | Min-conflicts steps per ant |
| `local-search-walk-probability` | `0.1` | Plateau escape |
| `stagnation-limit` | `40` | Iterations before τ reset |
| `seed` | *(unset)* | Set for byte-for-byte reproducible runs |

---

## 6. Measured behaviour

400 events (mixed 1h/2h), 12 rooms, 45 timeslots, ~12 conflict-graph neighbours per event —
**96.9% room-slot utilisation** (523 needed of 540 available):

| | Cost | Conflicting pairs |
|---|---|---|
| Random assignment (what the old inner loop effectively sampled) | 3260 | 326 |
| **ACO + min-conflicts** | **0** | **0** |

Solved in **188 ms**, verified by an independent recount of the raw assignment. Easy and moderately
loaded instances are typically solved during the first iteration by construction + local search;
the pheromone layer earns its keep on tight and over-subscribed instances, where it keeps improving
across iterations instead of resampling blindly.

---

## 7. Entry points

### `solve(UUID academicPeriodId)`
Builds the model, runs the solver, persists `eventTimeslot` + `eventRoom`, returns
`Map<UUID, EventAssignment>`.

> **The academic period is a required argument.** The previous signature took none and loaded every
> event in the school, so generating one period silently rescheduled all the others.

> **Only the block's first timeslot is persisted** — `Event` has a single `eventTimeslot` FK. The
> real extent is recovered by reading `eventDuration` forward from that start. Treat
> `eventTimeslot` as *"start of block"*, never *"the whole booking"*. Persisting the full block
> needs a schema change (an event↔timeslot join table).

### `generateTimetable(request)`
Seeds default timeslots if the school has none, resolves the period (fails fast on a bad id, before
a ten-minute run), calls `solve()`, and maps the result to sorted `TimetableEntryDTO`s.

### `downloadTimetablePdf(request)` / `getConflictGraphDot(request)`
Read already-persisted results. The DOT endpoint uses the same `ConflictGraphBuilder` as the solver,
so the picture always matches what was actually solved.

---

## 8. Tests

`src/test/java/.../solver/` — no database, no Spring context; the solver is a plain algorithm over
`int`s, so its tests are too.

- **`ConflictCounterTest`** — the two hard rules, distinct-pair counting, incremental correctness.
  If `CONFLICTS(...)` miscounts, the search optimises the wrong thing while still looking like it
  works, so these assert the rules directly rather than through the solver.
- **`PheromoneMatrixTest`** — evaporation, deposit, and the τ bounds.
- **`AcoTimetableSolverTest`** — solves tight and large instances (verified by independent recount),
  reproducibility under a fixed seed, best-effort output on provably unsatisfiable instances,
  Time_Limit adherence, unschedulable-event reporting, and beating a random baseline.

```bash
./mvnw test -Dtest='ConflictCounterTest,PheromoneMatrixTest,AcoTimetableSolverTest'
```

### Worth knowing when writing more

Conflicting events need **distinct timeslots**, and extra rooms cannot help — so an instance with
`k` mutually-conflicting events needs **≥ k timeslots** to be satisfiable at all. It is easy to
write a test that is accidentally pigeonhole-infeasible and then blame the solver.
