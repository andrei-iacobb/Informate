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

# List the built JARs for debugging
RUN ls -lh /app/target/*.jar

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the shaded jar specifically (original-* files are the unshaded versions)
# Maven Shade Plugin replaces the original JAR, so we copy informate-1.0-SNAPSHOT.jar
COPY --from=build /app/target/informate-1.0-SNAPSHOT.jar app.jar

# Verify the JAR has a manifest
RUN jar -xf app.jar META-INF/MANIFEST.MF && cat META-INF/MANIFEST.MF && rm -rf META-INF

# Create directories for data persistence
RUN mkdir -p /app/SiteImages

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
