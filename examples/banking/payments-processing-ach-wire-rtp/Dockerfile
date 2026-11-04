# Build:  mvn -B package && docker build -t banking-payments-processing-ach-wire-rtp .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/banking-payments-processing-ach-wire-rtp-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
