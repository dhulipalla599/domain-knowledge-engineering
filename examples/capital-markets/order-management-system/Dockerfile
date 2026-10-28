# Build:  mvn -B package && docker build -t capital-markets-order-management-system .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/capital-markets-order-management-system-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
