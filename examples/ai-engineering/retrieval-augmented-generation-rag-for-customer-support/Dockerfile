# Build:  mvn -B package && docker build -t ai-engineering-retrieval-augmented-generation-rag-for-customer-support .
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/ai-engineering-retrieval-augmented-generation-rag-for-customer-support-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
