# Build:  mvn -B package && docker build -t capital-markets-trade-execution-and-smart-order-routing .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/capital-markets-trade-execution-and-smart-order-routing-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
