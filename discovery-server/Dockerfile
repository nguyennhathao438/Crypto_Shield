FROM eclipse-temurin:26-jdk-ubi10-minimal

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java","-jar","app.jar"]