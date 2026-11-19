# Build:  mvn -B package && docker build -t banking-card-authorization-and-settlement .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/banking-card-authorization-and-settlement-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
