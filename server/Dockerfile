# Dungeon Delvers game server — single JAR (modular monolith, ADR-001).
# Build:  docker build -t ddo-server server/
# Run:    docker run -p 8080:8080 -e DB_URL=... -e DB_USER=... -e DB_PASSWORD=... -e JWT_SECRET=... ddo-server

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
# Dependencies first so they are cached between code changes.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 ddo
COPY --from=build /src/target/dungeon-delvers-server-*.jar app.jar
USER ddo
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=dev \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
# Health: GET /health (configure it in the hosting platform; the JRE image may lack curl/wget).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
