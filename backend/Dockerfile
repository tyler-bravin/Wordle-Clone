# --- Build stage -----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies separately from source so `mvn dependency:go-offline`
# only reruns when pom.xml actually changes.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# --- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Alpine-based images deliberately ship minimal - wget isn't guaranteed to be
# present, and the HEALTHCHECK below depends on it. Installing it explicitly
# here means the healthcheck can't silently fail just because a base image
# update drops a tool it was never guaranteed to include in the first place.
RUN apk add --no-cache wget && \
    addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/target/wordle.jar app.jar
RUN chown spring:spring app.jar
USER spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
