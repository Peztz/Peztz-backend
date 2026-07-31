FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

COPY src ./src

RUN ./gradlew clean test bootJar --no-daemon \
    && cp build/libs/spring-server-0.0.1-SNAPSHOT.jar /workspace/app.jar


FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system peztz \
    && useradd --system --gid peztz --create-home peztz

WORKDIR /app

COPY --from=builder /workspace/app.jar /app/app.jar

USER peztz

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
