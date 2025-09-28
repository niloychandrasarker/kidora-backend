# Stage 1: Build the app
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn

# Ensure mvnw is executable
RUN chmod +x mvnw

COPY src ./src

# Build the jar without running tests
RUN ./mvnw clean package -DskipTests

# Stage 2: Use slim JRE for running
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the jar from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
