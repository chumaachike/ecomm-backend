FROM maven:3.8.4-openjdk-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source files and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Use a slim runtime for the final image
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built jar file from the build stage
COPY --from=build /app/target/sb-ecom-0.0.1-SNAPSHOT.jar .

# Expose the application's port
EXPOSE 8080

# Define the entry point for the application
ENTRYPOINT ["java", "-jar", "sb-ecom-0.0.1-SNAPSHOT.jar"]
