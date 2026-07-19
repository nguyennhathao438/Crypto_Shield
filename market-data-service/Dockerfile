FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app

COPY pom.xml .
COPY market-data-service/pom.xml market-data-service/

RUN mvn -f market-data-service/pom.xml dependency:go-offline -B

COPY market-data-service/src ./market-data-service/src
RUN mvn -f market-data-service/pom.xml clean package -DskipTests -B

FROM eclipse-temurin:26-jre-alpine
WORKDIR /app
COPY --from=build /app/market-data-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]