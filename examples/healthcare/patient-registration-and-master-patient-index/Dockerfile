# Build:  mvn -B package && docker build -t healthcare-patient-registration-and-master-patient-index .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/healthcare-patient-registration-and-master-patient-index-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
