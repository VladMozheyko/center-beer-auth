# Реализация JWT с Spring Boot 3 и Spring Security 6

Этот репозиторий демонстрирует проект, который демонстрирует реализацию JSON Web Tokens (JWT) с Spring Boot 3 и Spring Security 6. Проект включает в себя следующие функциональности:

- Регистрация пользователя и вход в систему с аутентификацией по JWT
- Токен обновления хранится в базе данных
- Авторизация на основе ролей с разрешениями
- Настроенная обработка отказа в доступе
- Интеграция документации OpenAPI (Swagger)

# Стек технологий

- Spring Boot 3
- Spring Security
- Spring Data JPA
- Пользовательская валидация Spring Boot
- JSON Web Tokens(JWT)
- BCrypt
- Maven
- OpenAPI(SpringDoc Impl)
- Lombok

# Начало работы
Чтобы начать работу с этим проектом, вам нужно будет установить следующее на своем локальном компьютере:
- JDK 17+
- Maven 3+
## Configure Spring Datasource, JPA, App properties
1. Clone the repository
2. Open src/main/resources/application.yml
```
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/db_security
    username: postgres
    password: ${POSTGRES_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: create
    show-sql: true
    properties:
      hibernate:
        format_sql: true
    database: postgresql
    database-platform: org.hibernate.dialect.PostgreSQLDialect
server:
  port: 8086
application:
  security:
    jwt:
      secret-key: 586B633834416E396D7436753879382F423F4428482B4C6250655367566B5970
      expiration: 86400000 # a day
      refresh-token:
        expiration: 604800000 # 7 days
```
## Build and run the Project
- Build the project: `mvn clean install`
- Run the project: `mvn spring-boot:run`

The application will be available at http://localhost:8086.

# Test project
## User registration endpoint

`POST http://localhost:8086/api/v1/auth/register`

![register](register.PNG)

For detailed documentation and testing of the APIs, access the Swagger UI by visiting:
```
http://localhost:8086/swagger-ui.html
```
