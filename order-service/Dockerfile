FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app

COPY pom.xml .
COPY order-service/pom.xml order-service/

RUN mvn -f order-service/pom.xml dependency:go-offline -B

COPY order-service/src ./order-service/src
RUN mvn -f order-service/pom.xml clean package -DskipTests -B

FROM eclipse-temurin:26-jre-alpine
WORKDIR /app
COPY --from=build /app/order-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]