# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk

# Set the working directory
WORKDIR /app

# Add the application's jar file to the container
COPY target/User-Management-Service-0.0.1-SNAPSHOT.jar /app/User-Management-Service.jar

# Expose the port that the application will run on
EXPOSE 8081

# Run the jar file
ENTRYPOINT ["java", "-jar", "User-Management-Service.jar"]
