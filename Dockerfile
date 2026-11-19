# --- Build stage: compile and package the Spring Boot jar ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# --- Runtime stage: minimal JRE, non-root user ---
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /src/target/sentinel-trade.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
