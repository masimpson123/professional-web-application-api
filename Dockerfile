# syntax=docker/dockerfile:1

FROM eclipse-temurin:23

WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:resolve

COPY src ./src

# Containers listen on 8080, like Cloud Run; outside Docker the app defaults to 8081.
ENV PORT=8080
EXPOSE 8080
CMD ["./mvnw", "test", "spring-boot:run"]
