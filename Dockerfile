################ Build stage ################
FROM maven:3-sapmachine-26 AS builder

WORKDIR /build

# Cache dependencies first.
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw.cmd ./
RUN mvn -B -q dependency:go-offline

# Copy sources and build.
COPY src src
COPY eclipse-formatter-profile.xml eclipse.importorder license-header ./
RUN mvn -B -q -DskipTests package \
    && cp target/backend-*-SNAPSHOT.jar /build/app.jar

################ Runtime stage ################
FROM eclipse-temurin:25-jre-noble

# Create non-root user with stable UID/GID.
RUN groupadd --system --gid 1001 kirja \
    && useradd --system --uid 1001 --gid 1001 --shell /usr/sbin/nologin kirja

WORKDIR /app
COPY --from=builder --chown=kirja:kirja /build/app.jar /app/app.jar

USER 1001

ENV SPRING_PROFILES_ACTIVE=cloud \
    PORT=10000 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
