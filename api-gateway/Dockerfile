FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app

COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/

RUN mvn -f api-gateway/pom.xml dependency:go-offline -B

COPY api-gateway/src ./api-gateway/src
RUN mvn -f api-gateway/pom.xml clean package -DskipTests -B

FROM eclipse-temurin:26-jre-alpine
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]