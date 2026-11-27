# Build:  mvn -B package && docker build -t retail-store-replenishment .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/retail-store-replenishment-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
