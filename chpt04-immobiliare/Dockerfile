# Use an OpenJDK image
FROM openjdk:17-jdk-alpine

# Add the application's JAR to the container
COPY target/chpt04-immobiliare-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/docker-google-ai-key.json service-account-key.json

# Expose the port your app runs on
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "/app.jar"]
