#FROM google/cloud-sdk:slim

FROM openjdk:21

# Add the application's JAR to the container
COPY target/chpt04-immobiliare-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/docker-google-ai-key.json service-account-key.json

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV GOOGLE_APPLICATION_CREDENTIALS="/service-account-key.json"

# Expose the application port (Fly.io uses 8080 by default)
EXPOSE 8080

# Run the app locally in docker
#ENTRYPOINT ["java", "-jar", "/app.jar"]

# Run the app on Fly.io
CMD ["java", "-Dserver.port=8080", "-Dio.netty.noNativeTransport=true", "-jar", "/app.jar"]