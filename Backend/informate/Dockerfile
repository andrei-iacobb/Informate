# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application with Maven Shade Plugin
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the shaded jar specifically
COPY --from=build /app/target/informate-1.0-SNAPSHOT.jar app.jar

# Create directories for data persistence
RUN mkdir -p /app/SiteImages /app/data

# Expose port
EXPOSE 8080

# Run the application from /app/data to store databases there
WORKDIR /app/data
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
