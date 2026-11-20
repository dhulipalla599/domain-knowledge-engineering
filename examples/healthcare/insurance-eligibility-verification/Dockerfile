# Build:  mvn -B package && docker build -t healthcare-insurance-eligibility-verification .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/healthcare-insurance-eligibility-verification-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
