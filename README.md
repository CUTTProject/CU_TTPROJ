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

The proper fix is to make generation asynchronous — return a job id immediately and let the client
poll. That is the change to make if the deployment needs to produce real timetables.

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
