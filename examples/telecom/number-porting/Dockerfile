# Build:  mvn -B package && docker build -t telecom-number-porting .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/telecom-number-porting-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
