FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app

COPY pom.xml .
COPY discovery-server/pom.xml discovery-server/

RUN mvn -f discovery-server/pom.xml dependency:go-offline -B

COPY discovery-server/src ./discovery-server/src
RUN mvn -f discovery-server/pom.xml clean package -DskipTests -B

FROM eclipse-temurin:26-jre-alpine
RUN apk add --no-cache netcat-openbsd
WORKDIR /app
COPY --from=build /app/discovery-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]