# Build:  mvn -B package && docker build -t __ARTIFACT_ID__ .
FROM eclipse-temurin:__JAVA_VERSION__-jre
WORKDIR /app
COPY target/__ARTIFACT_ID__-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
