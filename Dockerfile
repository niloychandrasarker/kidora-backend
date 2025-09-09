# ===== Build Stage =====
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY .mvn .mvn
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# ===== Run Stage =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy the built jar (adjust pattern if final name differs)
COPY --from=build /app/target/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]