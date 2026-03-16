# Этап сборки проекта
FROM maven:3.8.7-eclipse-temurin-17 AS build
WORKDIR /app

# Копируем pom.xml и качаем зависимости заранее
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходники
COPY src/ /app/src/

# Собираем проект
RUN mvn clean package -DskipTests

# Финальный образ с JRE
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Копируем готовый jar из стадии билда
COPY --from=build /app/target/*.jar app.jar

# Копируем keystore.p12 в контейнер
COPY keystore.p12 /app/keystore.p12

# Экспонируем HTTPS порт
EXPOSE 8443

# Запускаем приложение
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
