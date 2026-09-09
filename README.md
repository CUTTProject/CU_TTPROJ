# CU_TTPROJ

University timetable scheduler — a multi-tenant Spring Boot service that generates conflict-free
timetables using a MAX-MIN Ant System with a min-conflicts local-search daemon.

## Running with Docker (recommended)

Everything comes up with one command — no MySQL install, no JDK, no `.env` sourcing.

```bash
cp .env.example .env      # then edit DB_PASSWORD and JWT_SECRET
docker compose up --build
```

The API is then on **http://localhost:8080**, with Swagger UI at
**http://localhost:8080/swagger-ui.html**.

Only two values in `.env` are actually used by Compose:

| Variable | Notes |
|---|---|
| `DB_PASSWORD` | becomes the MySQL root password **and** the app's datasource password |
| `JWT_SECRET` | must be ≥ 32 bytes — generate with `openssl rand -base64 48` |
| `JWT_EXPIRATION` | optional, defaults to `3600s` |

`DB_URL` from `.env` is **ignored**. It points at `localhost`, which is wrong inside the Compose
network — the app reaches MySQL at host `db`, and `docker-compose.yml` sets that itself.

Useful commands:

```bash
docker compose logs -f app     # follow the solver's output
docker compose down            # stop; the database volume survives
docker compose down -v         # stop and wipe the database
```

MySQL is also published on **host port 3307** so you can inspect the schema with a local client
during a demo.

### A note on CPU allocation

The solver's budget is **wall-clock** (`timetable.solver.time-limit-seconds`, default 600), not a
work budget. Throttling the container does not make it run longer — it completes **fewer iterations
in the same 600 seconds** and returns a worse timetable, silently.

`docker-compose.yml` allocates `cpus: "2.0"`. Lower it and generation quality drops. If you
benchmark, report the CPU allocation alongside the results.

## Deploying to Render

The repo contains a [Blueprint](render.yaml), so Render creates the database and the web service
together.

1. Push this branch to GitHub.
2. Render dashboard → **New → Blueprint** → select the repo. It reads `render.yaml`.
3. When prompted, set the one secret the blueprint does not contain:

   ```bash
   openssl rand -base64 48      # paste as JWT_SECRET
   ```

4. Apply. First build takes 5–10 minutes (Maven downloads everything once).

Render injects the Postgres host, port, database, user and password automatically — the app assembles
the JDBC URL from those parts, which is why `application.properties` supports `DB_HOST`/`DB_PORT`/
`DB_NAME` as well as a whole `DB_URL`.

Deploy it as a **Blueprint**, not as a hand-created Web Service: Render only reads `render.yaml` for
the former, so a service created by hand starts with none of those variables set. There is no
`localhost` fallback for the database — the app refuses to start and names the variables it is
missing, rather than dialling a database that is not there and failing several seconds later with
Hibernate's `Unable to determine Dialect without JDBC metadata`, which names nothing useful.

To point the deployment at an external Postgres (Neon, Supabase) instead, set `DB_URL` — a JDBC URL,
so rewrite Render's or the provider's `postgresql://user:pass@host/db` form as
`jdbc:postgresql://host:5432/db` with the credentials in `DB_USERNAME`/`DB_PASSWORD`. Add
`?sslmode=require` for any host reached over the public internet; Render's *internal* host does not
need it.

### What is different about the hosted instance

| | Local | Render free tier |
|---|---|---|
| Solver budget | 600s | **45s** |
| CPU | full core(s) | shared, throttled |
| Idle behaviour | always up | spins down after 15 min, ~50s cold start |
| Postgres | unlimited | free tier **expires after 30 days** |

The budget is cut to 45 seconds because `POST /timetable/generate` blocks synchronously, and a
10-minute request is terminated by Render's router long before the solver returns. Since the budget
is **wall-clock**, fewer seconds means fewer ant iterations and measurably worse timetables.

**Benchmark locally at 600s. Treat the hosted instance as a functional demo, not as evidence.**

Generation is now asynchronous, which is what makes the full budget usable: `POST /timetable/generate`
returns a job id immediately and the finished timetable is delivered to the school's webhook, so no
proxy timeout constrains the solve. See **Webhooks** below.


## Webhooks

Generation runs in the background, so results are pushed rather than returned inline. Each school
registers one URL and receives every event on it.

```
PUT /api/schools/webhook     {"webhookUrl": "https://example.com/hooks/timetable"}
```

The response contains `webhookSecret` **once**. It cannot be read back — rotate with
`{"rotateSecret": true}` if you lose it. `POST /api/schools/webhook/test` sends a single
`WEBHOOK_TEST` delivery and reports the status code your endpoint returned, so you can verify your
handler without waiting out a ten-minute solve.

Every delivery is a POST with this body:

```json
{ "event": "GENERATED_TIMETABLE", "deliveryId": "…", "schoolId": "…",
  "timestamp": "2026-09-09T10:15:00Z", "data": { } }
```

| Event | Meaning |
|---|---|
| `WEBHOOK_TEST` | Sent only by the test endpoint. |
| `GENERATION_STARTED` | Job accepted; carries the problem size and how many events have no possible slot. |
| `GENERATED_TIMETABLE` | The schedule. `feasible` says whether it is conflict-free; `stopReason` says why the solver stopped. |
| `TIMETABLE_CONFLICTS` | What the produced timetable still violates — room clashes, lecturer/student clashes, unplaced events. |
| `UNSCHEDULABLE_EVENTS` | Events no room-and-slot combination can satisfy. A data problem: more solver time will not help. |
| `CONFLICT_MAP` | The structural conflict graph — which sections can never share a slot, and why. |
| `GENERATION_FAILED` | The run did not produce a timetable. |
| `BULK_UPLOAD_RESULT` | A CSV or JSON upload committed. |

### Verifying a delivery

**Do this before trusting a payload.** The URL is yours, but nothing stops anyone else POSTing to it.

Headers: `X-Timetable-Event`, `X-Timetable-Delivery`, `X-Timetable-Timestamp`, and
`X-Timetable-Signature: t=<epoch>,v1=<hex>`.

The signature is `HMAC-SHA256(secret, "<timestamp>.<raw body>")`, hex-encoded. Compute it over the
**raw bytes**, before any JSON parsing — re-serialising the body changes it.

```python
expected = hmac.new(secret.encode(), f"{ts}.".encode() + raw_body, hashlib.sha256).hexdigest()
if not hmac.compare_digest(expected, received):   # constant-time
    return 401
```

Also reject deliveries whose timestamp is more than a few minutes old — the timestamp is inside the
signed string precisely so it cannot be rewritten — and deduplicate on `X-Timetable-Delivery`, which
stays the same across retries of one event.

A delivery is retried up to 3 times (2s then 8s) on a timeout or a 5xx. A 4xx is treated as final
and is not retried. Return 2xx as soon as you have stored the payload; do the work afterwards.

> **Deliveries are best effort and are not persisted.** If your endpoint is down for all three
> attempts, that notification is gone — but the timetable itself is not. It is written to the
> database and remains readable via the API.


## Running without Docker

Requires JDK 17 and a local MySQL 8.

```bash
cp .env.example .env                       # edit the values
set -a && source .env && set +a
./mvnw spring-boot:run
```

**Quote every value in `.env`.** `DB_URL` contains `&`, which the shell treats as a control
operator — unquoted, `source .env` fails part-way and exports *nothing*, and the app then dies on a
missing-placeholder error. Compose does not have this problem.

## Tests

```bash
./mvnw clean test
```

34 tests: the solver suite runs as a plain algorithm over `int`s with no database or Spring context;
`TenantIsolationTest` runs against in-memory H2.

## Documentation

| Document | Covers |
|---|---|
| `docs/ALGORITHM.md` | the solver by component |
| `docs/GENERATION_PIPELINE.md` | the full generate call in execution order, step by step |
| `docs/WORKED_EXAMPLE.md` | a hand-traced four-iteration run with every pheromone value |
| `docs/REFACTOR.md` | what was changed in the security/refactor pass, and why |
| `docs/PRD.md` | product requirements, written for frontend work |
