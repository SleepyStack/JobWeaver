# ── Stage 1: build ───────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy only the POM first to cache dependencies
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copy source and build the fat jar (skip tests – they ran in CI)
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ── Stage 2: runtime ────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Security: run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the fat jar from the build stage
COPY --from=builder /build/target/jobweaver-scheduler-0.0.1-SNAPSHOT.jar app.jar

# Own the workdir
RUN chown -R appuser:appgroup /app

USER appuser

ENTRYPOINT ["java", \
  "-Duser.timezone=Asia/Kolkata", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
