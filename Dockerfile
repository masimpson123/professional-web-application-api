# syntax=docker/dockerfile:1

FROM eclipse-temurin:23

WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:resolve

COPY src ./src

EXPOSE 8080
CMD ["./mvnw", "test", "spring-boot:run"]
