# ── Build stage ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Cache Maven deps before copying source
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q

# Build the fat jar
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ── Runtime stage ───────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user for security
RUN addgroup -S byline && adduser -S byline -G byline
USER byline

WORKDIR /app

# Copy the fat jar from build stage
COPY --from=build /workspace/target/*.jar app.jar

# Prefer IPv4 to avoid Docker networking issues
ENV JAVA_OPTS="-Djava.net.preferIPv4Stack=true \
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
