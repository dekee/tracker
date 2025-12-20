FROM gradle:8.5-jdk17 AS build
WORKDIR /workspace
COPY build.gradle.kts settings.gradle.kts gradlew gradlew.bat /workspace/
COPY gradle /workspace/gradle
COPY src /workspace/src
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
EXPOSE 8085
ENV SERVER_PORT=8085
ENTRYPOINT ["java","-jar","/app/app.jar"]

