# Build:  mvn -B package && docker build -t data-engineering-batch-etl-and-workflow-orchestration .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/data-engineering-batch-etl-and-workflow-orchestration-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
