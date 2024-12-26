# Use an OpenJDK image
FROM openjdk:17-jdk-alpine

# Add the application's JAR to the container
COPY target/chpt04-immobiliare-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your app runs on
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "/app.jar"]
