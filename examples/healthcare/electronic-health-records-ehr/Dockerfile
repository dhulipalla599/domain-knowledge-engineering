# Build:  mvn -B package && docker build -t healthcare-electronic-health-records-ehr .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/healthcare-electronic-health-records-ehr-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
