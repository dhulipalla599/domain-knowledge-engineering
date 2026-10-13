# Build:  mvn -B package && docker build -t insurance-quote-and-rating .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/insurance-quote-and-rating-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
