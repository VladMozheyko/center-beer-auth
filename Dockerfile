# Используем официальный образ Maven для сборки проекта
FROM maven:3.8.7-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ /app/src/
RUN mvn clean package -DskipTests

# Финальный образ с JRE
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

# Передаем JAVA_OPTS из docker-compose для настройки производительности
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
