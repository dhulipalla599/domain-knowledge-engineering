# Build:  mvn -B package && docker build -t capital-markets-clearing-and-settlement .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/capital-markets-clearing-and-settlement-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
