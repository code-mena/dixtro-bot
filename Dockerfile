FROM gradle:8.11.1-jdk21 AS build

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts /app/
COPY gradlew /app/
COPY gradle /app/gradle

COPY src /app/src

RUN ./gradlew build --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
