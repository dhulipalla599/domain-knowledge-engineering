# Build:  mvn -B package && docker build -t data-engineering-change-data-capture-cdc-pipelines .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/data-engineering-change-data-capture-cdc-pipelines-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
