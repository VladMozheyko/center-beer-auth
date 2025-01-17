# Gomind: серверная часть приложения для отопительных систем

## Реализация JWT с Spring Boot 3 и Spring Security 6

Этот репозиторий демонстрирует проект, который демонстрирует реализацию 
JSON Web Tokens (JWT) с Spring Boot 3 и Spring Security 6. 
## Функциональность проекта

  - Регистрация пользователя и вход в систему с аутентификацией по JWT
  - Токен обновления хранится в базе данных
  - Авторизация на основе ролей с разрешениями
  - Настроенная обработка отказа в доступе
  - Интеграция документации OpenAPI (Swagger)
    
## Стек технологий

  - ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-green) Spring Boot 3
  - ![Spring Security](https://img.shields.io/badge/Spring%20Security-blue) Spring Security
  - ![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-orange) Spring Data JPA
  - ![Пользовательская валидация Spring Boot](https://img.shields.io/badge/Custom%20Validation-yellow) Пользовательская валидация Spring Boot
  - ![JWT](https://img.shields.io/badge/JWT-red) JSON Web Tokens (JWT)
  - ![BCrypt](https://img.shields.io/badge/BCrypt-purple) BCrypt
  - ![Maven](https://img.shields.io/badge/Maven-blueviolet) Maven
  - ![OpenAPI](https://img.shields.io/badge/OpenAPI-lightgrey) OpenAPI (SpringDoc Impl)
  - ![Lombok](https://img.shields.io/badge/Lombok-darkblue) Lombok

## Локальное начало работы

  1) Чтобы начать работу с этим проектом, вам нужно будет установить следующее на своем локальном компьютере:
     - JDK 17+ (указано в файле pom.xml)
     - IDE (желательно IntelliJ IDEA)
     - SDK, которую я использую:
       ![register](images_for_readme/sdk.png)
  
  2) В файле application.properties поменять username и password MySQL c глобального на свой локальный:
     ```properties
     #Это:
     spring.datasource.username=kotitonttu
     spring.datasource.password=Eq13034513!
     ```
     ```properties
     #Заменить например на это:
     spring.datasource.username=root
     spring.datasource.password=sasha
     ```
  3) Создать в консоли или через графический интерфейс посредством приложения "MySQL Workbench" новую пустую схему с названием "vuary" согласно настройке:
     ```properties
     spring.datasource.url=jdbc:mysql://127.0.0.1:3306/vuary?serverTimezone=Europe/Moscow
     ```
     ![mysqlcreateschema](images_for_readme/mysqlcreateschema.png)
  
  4) Запустить сервер через уже настроенную конфигурацию "SecurityApplication"

 ## Перезапуск удаленного сервера

  1. Открыть окошко с командой одновременно нажмите клавишу Windows и кнопку R
  2. Наберите в строке cmd, а затем зажмите комбинацию Ctrl+Shift+Enter
  3. Входим на сервер через команду:
     ```bash
     ssh root@31.129.102.70
     ```
  4. Потом попросит пароль, вводим:
     ```bash
     Qq13037613!
     ```
  5. Перезапускаем сервер:
     ```bash
     cd go-mind
     sh shutdown.sh
     tail -f log.txt
     ctrl + c
     cd ~
     rm -r go-mind
     git clone https://github.com/aalleexxaannddrr-prog/go-mind.git
     cd go-mind
     mvn wrapper:wrapper
     ./mvnw spring-boot:run
     ctrl + c
     sh startup.sh
     tail -f log.txt
     ctrl + c
     exit
     ```

 ## Вход в MySQL на удаленном сервере

  1. Открыть окошко с командой одновременно нажмите клавишу Windows и кнопку R
  2. Наберите в строке cmd, а затем зажмите комбинацию Ctrl+Shift+Enter
  3. Входим на сервер через команду:
     ```bash
     ssh root@31.129.102.70
     ```
  4. Потом попросит пароль, вводим:
     ```bash
     Qq13037613!
     ```
  5. Входим в MySQL через команду:
     ```bash
     mysql -u root -p
     ```
  6. Вводим пароль:
     ```bash
     adminQq13037613!
     ```         
  7. Не забыть указать используемую схему "vuary" для запросов:
     ```sql
     use vuary;
     ```
  *
    ```
    -- Удаляем схему (базу данных) если она существует
    DROP DATABASE IF EXISTS gomind;

    -- Создаем новую пустую схему (базу данных) с тем же именем
    CREATE DATABASE gomind;
    ```
# Локальное тестирование посредством Postman 
## Этот endpoint позволяет пользователю зарегистрироваться (не забыть почистить cookies) и изменить путь сохранения с глобального на локальный в классе StorageService.


`POST http://localhost:8080/api/register`
## Фрагмент класса:
![storage_service](images_for_readme/storage_service.png)
## Пример тестирования:
![api_register](images_for_readme/api_register.png)
```json
{
"firstname": "Sasha",
"lastname": "Kichmarev",
"email": "kichmarev@list.ru",
"password": "Sasha123!",
"phoneNumber":"+78005553555",
"dateOfBirth" : "2000-01-01",
"workerRole" : "SELLER"
}
```
## Этот endpoint позволяет активировать пользователя для того, чтобы можно было потом войти в систему.
`POST http://localhost:8080/api/activate`
## Пример тестирования:
![api_activate](images_for_readme/api_activate.png)
```json
{
    "code": "1802"
}
```
## Этот endpoint позволяет пользователю войти в систему.
`POST http://localhost:8080/api/login`
## Пример тестирования:
![api_login](images_for_readme/api_login.png)
```json
{
    "email": "kichmarev@list.ru",
    "password": "Sasha123!"
}
```

## Этот endpoint позволяет обновить токен через json запрос.
`POST http://localhost:8080/api/refresh-token`
## Пример тестирования:
![api_refresh_token](images_for_readme/api_refresh_token.png)
```json
{
    "refreshToken": "NGJkZjMwYmEtMzlkOS00ODA2LWE3N2QtYWYxMWY1MzQ0MjRh"
}
```

## Этот endpoint позволяет обновить токен с использованием куки.
`POST http://localhost:8080/api/refresh-token-cookie`
## Пример тестирования:
![api_refresh_token_cookie](images_for_readme/api_refresh_token_cookie.png)

## Этот endpoint позволяет пользователю выйти из системы.
`POST http://localhost:8080/api/logout`
## Пример тестирования:
![api_logout](images_for_readme/api_logout.png)

## Этот endpoint возвращает профиль пользователя на основе предоставленного куки.
`GET http://localhost:8080/api/profile`
## Пример тестирования:
![api_profile](images_for_readme/api_profile.png)

## Этот endpoint возвращает данные пользователя на основе предоставленного куки.
`GET http://localhost:8080/api/user`
## Пример тестирования:
![api_user](images_for_readme/api_user.png)

Для получения подробной документации локально и тестирования API перейдите в Swagger UI, пройдя по ссылке, предварительно изменив ip адресс удаленного сервера на localhost в классе OpenAPIConfiguration:
```
http://localhost:8080/swagger-ui.html
```
![swagger](images_for_readme/swagger.png)

## Чтобы загрузить фотографии на сервер через графический интерфейс в ос Windows, вы можете использовать WinSCP. Вот пошаговое руководство:
1. Установите WinSCP
Перейдите на официальный сайт WinSCP.
Скачайте установочный файл и установите WinSCP, следуя инструкциям установщика.
2. Настройка соединения в WinSCP
Запустите WinSCP: Найдите ярлык на рабочем столе или в меню "Пуск" и запустите WinSCP.

Создайте новое соединение:

В открывшемся окне настройки сессии введите необходимые данные:

-  File protocol: SCP (или SFTP, если ваш сервер поддерживает его)
-  Host name: IP-адрес вашего сервера, например 31.129.102.70
-  Port number: 22 (по умолчанию для SSH)
-  User name: ваше имя пользователя на сервере
-  Password: ваш пароль на сервере
-  Пример заполнения в нашем случае:
![WinSCP](images_for_readme/WinSCP.png)
