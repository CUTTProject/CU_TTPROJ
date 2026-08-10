# ─────────────────────────────────────────────────────────────────────────────
# Build stage — full JDK and Maven, discarded once the jar exists.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS build

WORKDIR /build

# Dependencies are resolved in their own layer so editing src/ does not force a
# re-download on every rebuild. Only pom.xml changes invalidate this.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/

# Tests are skipped here on purpose: the image build is not the place to run a
# 15-second solver test. Run ./mvnw clean test locally or in CI instead.
RUN ./mvnw -B -q clean package -DskipTests


# ─────────────────────────────────────────────────────────────────────────────
# Runtime stage — JRE only, no Maven, no source, no .m2 cache.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre

# Run as a non-root user. Nothing in the app needs root.
RUN useradd --system --create-home --uid 1001 app

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN chown app:app app.jar

USER app
EXPOSE 8080

# MaxRAMPercentage lets the JVM size its heap from the container's memory limit
# rather than the host's. The solver holds the pheromone matrix and a
# roomCount x slotCount occupancy table, both of which are real heap on a large
# school, so leaving this at the default is a false economy.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
