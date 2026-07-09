FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app

COPY pom.xml .
COPY wallet-service/pom.xml wallet-service/

RUN mvn -f wallet-service/pom.xml dependency:go-offline -B

COPY wallet-service/src ./wallet-service/src
RUN mvn -f wallet-service/pom.xml clean package -DskipTests -B

FROM eclipse-temurin:26-jre-alpine
WORKDIR /app
COPY --from=build /app/wallet-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]