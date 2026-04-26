# Use Java image
FROM openjdk:17-jdk-slim

# Copy project files
COPY . /app

# Set working directory
WORKDIR /app

# Build project
RUN ./mvnw clean package || mvn clean package

# Run jar file
CMD ["java", "-jar", "target/*.jar"]