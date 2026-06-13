FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY backend/pom.xml ./
COPY backend/src ./src

RUN mvn -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

# Used by Docker healthcheck to verify backend readiness.
RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
COPY script_mock_data /script_mock_data/
ENV SCRIPT_MOCK_DATA_DIR=/script_mock_data/data
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
