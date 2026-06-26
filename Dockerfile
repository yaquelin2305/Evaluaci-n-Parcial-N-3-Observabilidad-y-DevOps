
FROM maven:3.9.8-eclipse-temurin-17-alpine AS build
WORKDIR /app


COPY pom.xml .


RUN mvn dependency:go-offline -B


COPY src ./src


RUN mvn clean package -DskipTests

 
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S devopsgroup && adduser -S devopsuser -G devopsgroup
USER devopsuser


COPY --from=build /app/target/*.jar app.jar


EXPOSE 8081


ENV DB_URL=jdbc:postgresql://tickets_db:5432/tickets_db


ENTRYPOINT ["java", "-jar", "app.jar"]