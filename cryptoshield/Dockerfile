FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app

COPY pom.xml .
COPY cryptoshield/pom.xml cryptoshield/

RUN mvn -f cryptoshield/pom.xml dependency:go-offline -B

COPY cryptoshield/src ./cryptoshield/src
RUN mvn -f cryptoshield/pom.xml clean package -DskipTests -B

FROM eclipse-temurin:26-jre-alpine
WORKDIR /app
COPY --from=build /app/cryptoshield/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]