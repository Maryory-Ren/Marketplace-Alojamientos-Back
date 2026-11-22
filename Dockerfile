# Build reproducible con Java 21 y Maven.
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

# Imagen de ejecución mínima.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S marketplace && adduser -S marketplace -G marketplace
COPY --from=build /workspace/target/marketplace-0.0.1-SNAPSHOT.jar app.jar
USER marketplace
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
