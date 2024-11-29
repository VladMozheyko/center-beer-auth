# Используем официальный образ с OpenJDK 17
FROM openjdk:17-jdk-alpine

# Устанавливаем рабочую директорию в контейнере
WORKDIR /app

# Копируем проект в контейнер
COPY . /app

# Выполняем сборку проекта с помощью Maven (если у вас нет собранного .jar файла)
RUN ./mvnw clean install

EXPOSE 8080

# Указываем команду для запуска вашего Spring Boot приложения
CMD ["./mvnw", "spring-boot:run"]
