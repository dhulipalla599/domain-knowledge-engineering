# Build:  mvn -B package && docker build -t ai-engineering-llm-gateway-and-prompt-management .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/ai-engineering-llm-gateway-and-prompt-management-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
