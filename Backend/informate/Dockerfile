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

# Debug: Show what JARs were created
RUN echo "=== JARs in target directory ===" && \
    ls -lh /app/target/*.jar && \
    echo "=== Checking manifest of informate-1.0-SNAPSHOT.jar ===" && \
    jar -xf /app/target/informate-1.0-SNAPSHOT.jar META-INF/MANIFEST.MF && \
    cat META-INF/MANIFEST.MF && \
    echo "=== End of manifest ===" || echo "Failed to extract manifest"

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the shaded jar specifically
COPY --from=build /app/target/informate-1.0-SNAPSHOT.jar app.jar

# Verify the JAR was copied
RUN ls -lh app.jar

# Create directories for data persistence
RUN mkdir -p /app/SiteImages

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
