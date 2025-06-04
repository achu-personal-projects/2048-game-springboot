# Use official OpenJDK image
FROM openjdk:17-jdk-slim
WORKDIR /app
# Copy built JAR file
COPY target/game-2048-1.0.0.jar game-2048.jar
# Expose application port
EXPOSE 8080
# Run the application
ENTRYPOINT ["java", "-jar", "game-2048.jar"]
