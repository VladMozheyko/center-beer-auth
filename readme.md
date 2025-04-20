# Go Mind  
**Приложение "Go Mind" - это викторина, в которой можно заработать своим умом. Отвечайте на вопросы быстрее остальных и получайте бонусы.**  

---

## 🛠 Технологический стек  

Эта система построена с использованием современных технологий для обеспечения высокой производительности, безопасности и масштабируемости.  

### 🔹 **Основные компоненты**  

| **Категория**        | **Технологии** |
|----------------------|---------------|
| **🖥️ Ядро**         | Java 17 • Spring Boot 3.1 • Spring Security 6 • Spring Data JPA |
| **🔐 Аутентификация** | JWT 0.11.5 • BCrypt • OAuth2 • Spring Security ACL |
| **🗄️ Базы данных**   | MySQL 8 • Hibernate 6 • Flyway Migrations • Redis 7 |
| **🌐 API**           | REST/JSON • OpenAPI 3 • WebSockets (Pusher 1.0) |
| **🔧 Утилиты**       | Lombok • MapStruct 1.4 • Apache POI 3.9 • OpenCSV 5.7 |
| **📦 Инфраструктура** | Docker 24 • NGINX 1.25 • GitHub Actions • Prometheus |

---

## 🔄 Обновление проекта на сервере  

Чтобы внести последние изменения, выполните следующие шаги:

### 1️⃣ **Загрузка SSH-ключей**  
Скачайте файлы SSH-ключей:  
🔗 [Ключ 1](https://drive.google.com/file/d/1q93OyIv5nmqhqlKl7V9l89rxnkOL_pio/view?usp=sharing)  
🔗 [Ключ 2](https://drive.google.com/file/d/1E_6AnxJhlAL4Z0-5zYfgfvZZwbPhrlbD/view?usp=sharing)  

Поместите их в папку:  
📂 `C:\Users\Asus\.ssh\`  

---

### 2️⃣ **Подключение к серверу**  
Откройте терминал (PowerShell или CMD) и выполните команду:  
```sh
ssh -i C:\Users\Asus\.ssh\GoMind admin@158.160.138.117
```
Введите пароль при запросе:
```sh
Vlad!123
```
### 3️⃣ Обновление кода и перезапуск сервера
После успешного входа введите следующие команды:
```sh
cd go-mind
git pull
docker-compose down
docker-compose down -v
docker-compose build --no-cache
docker-compose up --build -d
docker-compose up -d
docker-compose logs -f
```
