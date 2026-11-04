# Build:  mvn -B package && docker build -t logistics-shipment-booking .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/logistics-shipment-booking-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
