# Use an OpenJDK image
FROM openjdk:17-jdk-alpine

# Add the application's JAR to the container
COPY target/chpt04-immobiliare-0.0.1-SNAPSHOT.jar app.jar

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Expose the port your app runs on
#EXPOSE 8080

# heroku needs this port from what i read
EXPOSE 5000

# Run the app
#ENTRYPOINT ["java", "-jar", "/app.jar"]

# Run the app on heroku
CMD ["sh", "-c", "java -Dserver.port=$PORT -jar /app.jar"]
