# Build:  mvn -B package && docker build -t airline-flight-shopping-and-booking .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/airline-flight-shopping-and-booking-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
