# Center Beer Auth — История изменений

## Правила

- Формат версий: `MAJOR.MINOR.PATCH` (например, `0.1.0`).
- Для каждой версии фиксируем **только**:
  - что нового;
  - что изменено;
  - что исправлено;
  - изменения в настройках/окружении;
  - что важно знать при деплое.
- Если пункта нет — раздел можно не писать.

Если вносим одно поле в сущность, то просто правим текущую версию дто, и все будет работать как в прямом так и в обратном порядке

<details>
<summary>Шаблон записи версии</summary>

### Шаблон записи версии
## [X.Y.Z] - YYYY-MM-DD
### Новое
- ...
### Изменено
- ...
### Исправлено
- ...
### Настройки / окружение
- ...
### Деплой
- ...k

</details>

---

<details>
<summary>Пример описания версии</summary>

## [0.1.0] - 2026-06-01

### Новое
- [API] Добавлен эндпоинт `POST /oauth2/vk_pkce_token` для завершения VK PKCE‑авторизации.
- [BE] Реализован сервис `VkPkceAuthService` для работы с VK PKCE‑потоком.
- [SECURITY] Добавлен фильтр `JwtAuthenticationFilter` для обработки JWT в запросах к защищённым эндпоинтам.

### Изменено
- [BE] Обновлён `VkIdPKCEController` — логика перенаправления и обработки кода вынесена в сервис.
- [BE] Упрощён `UserCreateService`: инициализация дефолтных директорий вынесена в отдельный компонент.
- [LOGS] Улучшены логи аутентификации и авторизации (добавлены `userId` и `provider` в debug‑сообщения).

### Исправлено
- [BUG][#AUTH-198] VK‑пользователь теперь создаётся при первом логине.
- [BUG][#AUTH-207] Исправлен таймаут при запросе к VK API, добавлен retry с backoff.

### Настройки / окружение
- Новые параметры:
  - `auth.vk.client-id`
  - `auth.vk.client-secret`
  - `auth.vk.redirect-uri`
  - `auth.vk.pkce-enabled` (boolean, default: `true`)
- Для профиля `test`:
  - Переключатель типа БД: `test.db.type=h2|tc|real`
  - Настройки H2:
    - `test.h2.datasource.url=jdbc:h2:mem:bm_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE`
    - `test.h2.datasource.username=sa`
    - `test.h2.datasource.password=`
- Для интеграционных тестов:
  - Введён `AbstractIntegrationTest` с выбором БД:
    - `test.db.type=h2` — H2 in‑memory (по умолчанию)
    - `test.db.type=tc` — MySQL Testcontainers
    - `test.db.type=real` — реальная MySQL по `application.properties`

### Деплой
- Прод:
  - Убедиться, что заданы:
    - `auth.vk.client-id`
    - `auth.vk.client-secret`
    - `auth.vk.redirect-uri`
- CI:
  - Юнит‑тесты: `test.db.type=h2`.
  - Интеграционные тесты: `test.db.type=tc`.
</details>

---



