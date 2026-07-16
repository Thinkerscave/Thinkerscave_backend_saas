# Multi-stage production image for Thinkerscave backend
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy AS runtime
ENV TZ=UTC \
    LANG=C.UTF-8 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -Dfile.encoding=UTF-8 -Duser.timezone=UTC" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home /app app \
    && mkdir -p /app/uploads /app/Logs \
    && chown -R app:app /app

WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar
USER app

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
