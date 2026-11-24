# Build:  mvn -B package && docker build -t data-engineering-data-quality-and-validation .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/data-engineering-data-quality-and-validation-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
