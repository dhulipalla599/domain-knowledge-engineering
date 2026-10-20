# Build:  mvn -B package && docker build -t telecom-subscriber-onboarding .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/telecom-subscriber-onboarding-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
