# Build:  mvn -B package && docker build -t airline-passenger-name-record-pnr .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/airline-passenger-name-record-pnr-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
