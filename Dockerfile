FROM gradle:8.10.2-jdk21 AS builder

WORKDIR /workspace

COPY build.gradle settings.gradle ./
COPY src src

RUN gradle --no-daemon bootJar

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
