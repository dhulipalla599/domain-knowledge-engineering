# Build:  mvn -B package && docker build -t insurance-policy-administration .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/insurance-policy-administration-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
