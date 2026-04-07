package fr.mossaab.security.controller;

import fr.mossaab.security.dto.social.SocialExchangeRequest;
import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.OAuthRequestStatus;
import fr.mossaab.security.exception.SocialAuthException;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.JwtService;
import fr.mossaab.security.service.RefreshTokenService;
import fr.mossaab.security.service.social.service.OneTimeAuthCodeService;
import fr.mossaab.security.service.social.service.SocialAccountLinkingService;
import fr.mossaab.security.service.social.service.UserRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для завершения OAuth2-аутентификации:
 * вход, регистрация или привязка социального аккаунта.
 */
@RestController
@RequestMapping("/oauth2/social")
@RequiredArgsConstructor
@Tag(name = "OAuth2", description = "Вход/регистрация/привязка через соцсети")
public class OAuth2Controller {

    private final OneTimeAuthCodeService oneTimeAuthCodeService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRegistrationService registration;
    private final SocialAccountLinkingService linkingService;

    @Operation(summary = "\uD83D\uDD27 Пошаговое руководство: регистрация, авторизация и привязка соцсетей (Google, Yandex)",
            description = """
                    ## 🛠️ Пошаговое руководство для подключения регистрации, авторизации и привязки профилей соцсетей
                    
                    ### 1. Создание кнопок авторизации на фронтенде
                    
                    **Действия разработчика фронтенда:**
                    * Создать кнопки для каждой соцсети с прямыми ссылками на эндпоинты бэкенда.
                    * Указать полные URL с базовым адресом API.
                    
                    **Базовые URL:**
                    * **Yandex:** `{API_BASE}/oauth2/authorization/yandex`
                    * **Google:** `{API_BASE}/oauth2/authorization/google`
                    
                    > **Примечание:** `{API_BASE}` — базовый URL API (например, `https://center.beer`).
                    
                    **Пример HTML‑реализации:**
                    
                    ```html
                    <div class="social-auth-buttons">
                      <button class="login-button google"
                              onclick="window.location.href='{API_BASE}/oauth2/authorization/google'">
                        Войти через Google
                      </button>
                      <button class="login-button yandex"
                              onclick="window.location.href='{API_BASE}/oauth2/authorization/yandex'">
                        Войти через Yandex
                      </button>
                    </div>
                    ```
                    
                    ### 2. Процесс авторизации через соцсети
                    
                    **Последовательность действий:**
                    1. Пользователь нажимает кнопку соцсети на фронтенде.
                    2. Браузер перенаправляет пользователя на эндпоинт бэкенда:
                       * `/oauth2/authorization/google` → инициирует OAuth‑поток с Google;
                       * `/oauth2/authorization/yandex` → инициирует OAuth‑поток с Yandex.
                    3. Бэкенд формирует и перенаправляет на страницу авторизации провайдера.
                    4. Пользователь авторизуется в соцсети и подтверждает разрешения.
                    5. Соцсеть редиректит пользователя обратно на бэкенд (указанный идентичный 'redirectUrl' в соцсети и на бекенде).
                    6. Бэкенд обрабатывает ответ от соцсети, генерирует одноразовый `authCode`.
                    7. Бэкенд редиректит пользователя на фронтенд с параметрами:
                       * `auth_status` — статус авторизации (`SOCIAL_FOUND`/`EMAIL_LINKED`/`NEW_SOCIAL_USER`/`NEW_ACCOUNT`/`ERROR`);
                       * `auth_code` — временный, одноразовый токен для дальнейших операций (действует только один раз и в течение 3 минут);
                       * `baseUserEmail` — email пользователя (если найден в системе);
                       * `provider` — провайдер (`GOOGLE`/`YANDEX`/`VK`).
                    
                    **Статусы и их описание:**
                    
                        * SOCIAL_FOUND - Аккаунт соцсети уже есть,
                    
                        * EMAIL_LINKED - Этот email соцсети уже используется в другой учётке,
                    
                        * NEW_SOCIAL_USER - Пользователь найден по email, socialId еще не привязан,
                    
                        * NEW_ACCOUNT - Пользователь новый, можно создать аккаунт,
                    
                        * ERROR - Критическая ошибка
                    
                    ### 3. Обработка ответа на фронтенде
                    
                    **Фронтенд должен:**
                    * принять GET‑запрос с параметрами редиректа;
                    * проанализировать `auth_status`;
                    * в случае успеха показать форму выбора действия или автоматически выполнить необходимый запрос.
                    
                    **Параметры запроса от бэкенда:**
                    
                    | Параметр | Тип | Описание | Пример |
                    |--------|---------|----------|--------|
                    | `auth_status` | `string` | Статус авторизации | `NEW_ACCOUNT` |
                    | `auth_code` | `string` | Одноразовый токен | `abc123xyz` |
                    | `baseUserEmail` | `string` | Email пользователя (опционально) | `user@example.com` |
                    | `provider` | `string` | Провайдер авторизации | `GOOGLE` |
                    
                    ### 4. Выбор действия и отправка запроса к API
                    
                    **Форма выбора действия (пример):**
                    
                    ```json
                    {
                      "authCode": "abc123xyz",
                      "provider": "GOOGLE"
                    }
                    ```
                    
                    **Эндпоинты для отправки:**
                    
                    | Действие | Эндпоинт | Метод | Назначение |
                    |--------|----------|-------|------------|
                    | **Регистрация** | `/oauth2/social/register` | POST | Создать новый аккаунт через соцсеть |
                    | **Авторизация** | `/oauth2/social/login` | POST | Войти в существующий аккаунт |
                    | **Привязка** | `/oauth2/social/link` | POST | Привязать соцсеть к текущему аккаунту |
                    
                    **Структура запроса к API:**
                    
                    ```json
                    {
                      "authCode": "abc123xyz",
                      "provider": "YANDEX"
                    }
                    ```
                    
                    **Поля запроса:**
                    
                    | Поле | Тип | Обязательное | Описание |
                    |------|---------|------------|----------|
                    | `authCode` | `string` | Да | Одноразовый код из редиректа |
                    | `provider` | `string` (`GOOGLE`, `YANDEX`, `VK`) | Да | Провайдер авторизации |
                    
                    !ВАЖНО при присоединении аккаунтов необходимо пользователю быть авторизированным и в куках должен посылатся JWT для получения данных о пользователе
                    
                    ### 5. Ответ бэкенда
                    
                    **В случае успеха:**
                    * JWT‑токен (`accessToken`);
                    * refresh‑токен (`refreshToken`);
                    * данные пользователя.
                    
                    **В случае ошибки:**
                    * код ошибки (`error`);
                    * сообщение (`message`).
                    """)
    @GetMapping("/guide")
    private void guide() {/* Только для документирования*/}

    @Operation(summary = "🔐 Пошаговое руководство: регистрация, авторизация и привязка профиля VK ID",
            description = """
                    ## 🛠️ Пошаговое руководство для подключения VK ID (с использованием PKCE)
                    ## В конце содержит полный пример реализации одностраничного web-приложения
                    
                    ### 1. Получение конфигурации VK ID с бэкенда
                    
                    **Действия разработчика фронтенда:**
                    * Отправить GET‑запрос на эндпоинт бэкенда для получения конфигурации VK ID.
                    * Использовать полученные данные для формирования PKCE‑запроса к VK.
                    
                    **Эндпоинт:**
                    * **`GET /oauth2/vk_id-config`**
                    
                    **Ответ бэкенда (пример):**
                    ```json
                    {
                      "clientId": "1234567",
                      "redirectUri": "{API_BASE}/oauth2/callback",
                      "scope": "email profile",
                      "authBackendUrl": "https://api.example.org" ---> это url который будет использовать backend для запросов
                    }
                    ```
                    
                    ### 2. Формирование PKCE‑запроса и авторизация через VK
                    
                    **Последовательность действий:**
                    1. Фронтенд генерирует `code_verifier` и `code_challenge` (PKCE).
                    2. Формируется URL для авторизации VK:
                       ```
                       https://oauth.vk.com/authorize?
                         client_id={clientId}&
                         redirect_uri={redirectUri}&
                         response_type=code&
                         scope={scope}&
                         state={state}&
                         code_challenge={code_challenge}&
                         code_challenge_method=S256
                       ```
                    
                    [VK ID INFO](https://id.vk.com/about/business/go/docs/ru/vkid/latest/vk-id/connection/create-application)
                    
                    Пример и полезные функции
                    
                    **Пример кода**
                    ```javascript
                            let params = getParams();document.getElementById('vk-login').onclick = async function () {
                                                         if (!vkConfig) {
                                                             showStatus("Конфигурация VK ещё не загружена. Подождите...", "red");
                                                             return;
                                                         }
                    
                                                         const { verifier, challenge } = await generatePKCE();
                                                         const state = uuid();
                    
                                                         localStorage.setItem("vk_code_verifier", verifier);
                                                         localStorage.setItem("vk_auth_state", state);
                    
                                                         const params = new URLSearchParams({
                                                             response_type: "code",
                                                             client_id: vkConfig.clientId,
                                                             redirect_uri: vkConfig.redirectUri,
                                                             scope: vkConfig.scope,
                                                             state: state,
                                                             code_challenge: challenge,
                                                             code_challenge_method: "S256"
                                                         });
                    
                                                         window.location = "https://id.vk.ru/authorize?" + params.toString();
                                                     };
                    
                    ```
                    
                    **Функция создаёт пару verifier/challenge:**
                    
                    ```javascript
                        async function generatePKCE() {
                            const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';
                            const arr = Array.from(crypto.getRandomValues(new Uint8Array(64)));
                            const verifier = arr.map(x => chars.charAt(x % chars.length)).join('');
                    
                            // Вычисление SHA-256
                            const data = new TextEncoder().encode(verifier);
                            const hashed = await crypto.subtle.digest("SHA-256", data);
                    
                            // Преобразование в base64url
                            const challenge = btoa(String.fromCharCode(...new Uint8Array(hashed)))
                                .replace(/\\+/g, '-').replace(/\\//g, '_').replace(/=+$/, '');
                    
                            return { verifier, challenge };
                        }
                    ```
                    - генерирует случайный verifier длиной 64 символа;
                    - вычисляет challenge через SHA‑256;
                    -кодирует результат в base64url (требование VK ID).
                    
                    **Генерация state (uuid)*
                    
                    ```javascript
                        function uuid() {
                            return ([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g, c =>
                                (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
                            );
                        }
                    ```
                    
                    3. Бекенд проксирует ответ от вк на frontend
                    4. Frontend должен сформировать запрос на бекенд
                    
                    ```json
                        {
                                "code":"xxxxxx"
                                "code_verifier":"xzxzxzzxxxzx"
                                "device_id":"xxxxxxxxxx"
                        }
                    ```
                    5. Backend 
                    
                    **Бэкенд выполняет:**
                    * принимает код авторизации от VK;
                    * обменивает `code` на `access_token` VK через эндпоинт VK API;
                    * запрашивает данные пользователя VK (`email`, `user_id`, `first_name`, `last_name`);
                    * создаёт временный объект пользователя с данными VK;
                    * генерирует одноразовый `authCode` (действует 3 минуты);
                    * редиректит пользователя на фронтенд с параметрами:
                      * `auth_status` — статус авторизации;
                      * `auth_code` — временный, одноразовый токен;
                      * `baseUserEmail` — email пользователя (если есть в VK);
                      * `provider` — `VK`;
                      * `vkUserId` — ID пользователя VK.
                    
                    **Используемые эндпоинты бэкенда:**
                    * **`POST /oauth2/vk_pkce_token`** — обмен `code` от VK на `auth_code`;
                    
                    **Статусы и их описание:**
                    
                        * SOCIAL_FOUND - Аккаунт соцсети уже есть,
                    
                        * EMAIL_LINKED - Этот email соцсети уже используется в другой учётке,
                    
                        * NEW_SOCIAL_USER - Пользователь найден по email, socialId еще не привязан,
                    
                        * NEW_ACCOUNT - Пользователь новый, можно создать аккаунт,
                    
                        * ERROR - Критическая ошибка
                    
                    ### 3. Обработка ответа на фронтенде
                    
                    **Фронтенд должен:**
                    * принять GET‑запрос с параметрами редиректа;
                    * проанализировать `auth_status`;
                    * в случае успеха показать форму выбора действия или автоматически выполнить необходимый запрос.
                    
                    **Параметры запроса от бэкенда:**
                    
                    | Параметр | Тип | Описание | Пример |
                    |--------|---------|----------|--------|
                    | `auth_status` | `string` | Статус авторизации | `NEW_ACCOUNT` |
                    | `auth_code` | `string` | Одноразовый токен | `abc123xyz` |
                    | `baseUserEmail` | `string` | Email пользователя (опционально) | `user@example.com` |
                    | `provider` | `string` | Провайдер авторизации | `GOOGLE` |
                    
                    ### 4. Выбор действия и отправка запроса к API
                    
                    **Форма выбора действия (пример):**
                    
                    ```json
                    {
                      "authCode": "abc123xyz",
                      "provider": "GOOGLE"
                    }
                    ```
                    
                    **Эндпоинты для отправки:**
                    
                    | Действие | Эндпоинт | Метод | Назначение |
                    |--------|----------|-------|------------|
                    | **Регистрация** | `/oauth2/social/register` | POST | Создать новый аккаунт через соцсеть |
                    | **Авторизация** | `/oauth2/social/login` | POST | Войти в существующий аккаунт |
                    | **Привязка** | `/oauth2/social/link` | POST | Привязать соцсеть к текущему аккаунту |
                    
                    **Структура запроса к API:**
                    
                    ```json
                    {
                      "authCode": "abc123xyz",
                      "provider": "YANDEX"
                    }
                    ```
                    
                    **Поля запроса:**
                    
                    | Поле | Тип | Обязательное | Описание |
                    |------|---------|------------|----------|
                    | `authCode` | `string` | Да | Одноразовый код из редиректа |
                    | `provider` | `string` (`GOOGLE`, `YANDEX`, `VK`) | Да | Провайдер авторизации |
                    
                    !ВАЖНО при присоединении аккаунтов необходимо пользователю быть авторизированным и в куках должен посылатся JWT для получения данных о пользователе
                    
                    ### 5. Ответ бэкенда
                    
                    **В случае успеха:**
                    * JWT‑токен (`accessToken`);
                    * refresh‑токен (`refreshToken`);
                    * данные пользователя.
                    
                    **В случае ошибки:**
                    * код ошибки (`error`);
                    * сообщение (`message`).
                    
                    ---
                    
                    ## 🔑 Ключевые отличия от Google/Yandex
                    
                    1. **PKCE‑поток** обязателен для VK ID.
                    2. **Предварительный запрос конфигурации** — фронтенд сначала получает `clientId`, `scope` и т.д. с бэкенда.
                    3. **Дополнительная проверка данных** — фронтенд может проверить данные пользователя VK перед отправкой на бэкенд.
                    4. **Уникальный параметр** — `vkUserId` в ответе бэкенда.
                    5. **Специфичный эндпоинт** — `/oauth2/vk_pkce_token` для обмена кода VK на `auth_code`.
                    
                    ## Пример реализации кода авторизации/привязывания/регистарции
                    
                    ```html
                    <html lang="en">
                    <body>
                    <div class="centerbox">
                        <h1>Вход в аккаунт</h1>
                        <div id="status-msg"></div>
                    
                        <!-- Форма логина по email/паролю -->
                        <form id="login-form" autocomplete="on">
                            <input type="email" id="email" autocomplete="username" placeholder="E-mail" required>
                            <input type="password" id="pass" autocomplete="current-password" placeholder="Пароль" required>
                            <button type="submit">Войти</button>
                        </form>
                    
                        <div class="or">или через соцсети</div>
                    
                        <!-- Кнопки социальных сетей: Google, Яндекс, ВКонтакте -->
                        <div class="soc-links">
                            <button class="soc-btn soc-google" id="google-login" type="button">
                                <svg viewBox="0 0 48 48"><g><path fill="#fff" d="M43.6 20.5h-1.6V20H24v8h11.2c-1.3 3.6-4.7 6-8.7 6a9 9 0 110-18c2.2 0 4.3.8 5.9 2.2l6.3-6.3A17 17 0 0024 7a17 17 0 100 34c8.7 0 16-7.1 16-16 0-1.1-.1-2.1-.4-3.1z"/><path fill="#4285F4" d="M6 14.6l6.6 4.8A9.1 9.1 0 0124 15c2.1 0 4.1.7 5.7 2.1l6.3-6.2C33.8 8.3 29.1 6 24 6c-6 0-11.3 2.7-15 6.9z"/><path fill="#34A853" d="M24 43c5 0 9.7-1.7 13.3-4.7l-6.6-5.5A9 9 0 0124 39c-3.7 0-7-2.1-8.6-5.3l-6.5 5C12.8 41.4 18.1 43 24 43z"/><path fill="#FBBC05" d="M9.4 27.6A9 9 0 0115.3 24c.2-1 .6-2.1 1.1-2.9l-6.5-5A17 17 0 007 24c0 2.1.4 4.1 1.1 5.9z"/><path fill="#EA4335" d="M43.6 20.5h-1.6V20H24v8h11.2c-.5 2.1-1.7 4-3.3 5.3l6.6 5.5c3.1-2.9 5.1-7.1 5.1-12.3 0-1.1-.1-2.1-.4-3.1z"/></g></svg>
                                Google
                            </button>
                    
                            <button class="soc-btn soc-ya" id="yandex-login" type="button">
                                <svg viewBox="0 0 48 48"><g><circle fill="#fff" cx="24" cy="24" r="24"/><path fill="#fc3" d="M24 4C13.5 4 5 12.5 5 23c0 5.4 2 10.4 5.7 14l3.5-3.2C10.8 30.2 9 26.7 9 23c0-8.3 6.7-15 15-15s15 6.7 15 15c0 3.7-1.8 7.2-5.2 10.8l3.5 3.2C41 33.4 43 28.4 43 23c0-10.5-8.5-19-19-19z"/><path fill="#fc3" d="M24 9c7.7 0 14 6.3 14 14s-6.3 14-14 14-14-6.3-14-14S16.3 9 24 9zm0-5C13.5 4 5 12.5 5 23c0 5.4 2 10.4 5.7 14l3.5-3.2C10.8 30.2 9 26.7 9 23c0-8.3 6.7-15 15-15s15 6.7 15 15c0 3.7-1.8 7.2-5.2 10.8l3.5 3.2C41 33.4 43 28.4 43 23c0-10.5-8.5-19-19-19z"/></g></svg>
                                Яндекс
                            </button>
                    
                            <button class="soc-btn soc-vk" id="vk-login" type="button">
                                Войти через VK ID
                            </button>
                        </div>
                    </div>
                    
                    <!-- Блок 3: Объединённый JavaScript — логика всех провайдеров -->
                    <script>
                        let vkConfig = null;
                        const API = "http://localhost"; // Базовый URL для Google/Yandex/внутренних API
                        const API_BASE = "http://localhost:80"; // Отдельный бэкенд для VK PKCE
                    
                        // --- Сообщения для OAuth-статусов ---
                        const socialAuthStatusLabels = {
                            SOCIAL_FOUND:   "Аккаунт соцсети уже есть (вы можете войти)",
                            EMAIL_LINKED:   "Email соцсети зарегистрирован (используйте логин/пароль)",
                            NEW_SOCIAL_USER:"Ваша учётка найдена по e-mail. Привязать соцсеть или что сделать?",
                            NEW_ACCOUNT:    "Такой e-mail не найден — вы можете зарегистрироваться через соцсеть",
                            ERROR:          "Ошибка авторизации через соцсеть"
                        };
                    
                        const oauthRequestStatusLabels = {
                            REGISTER: "Зарегистрировать новый аккаунт",
                            LOGIN:    "Войти как найденный пользователь (связать и войти)",
                            LINK:     "Привязать соцсеть к текущей учётке"
                        };
                    
                        // --- Показ сообщений ---
                        function showStatus(msg, color) {
                            const el = document.getElementById("status-msg");
                            el.innerHTML = msg || "";
                            el.style.background = color === "red"
                                ? "#ffe9ec" : color === "green"
                                    ? "#eaffee" : "#f6faff";
                            el.style.color = color === "red"
                                ? "#b2001c" : color === "green"
                                    ? "#117a38" : "#1d415d";
                        }
                    
                        // --- Парсинг параметров из URL ---
                        function getQueryParams() {
                            let params = {}, s = window.location.search.replace('?', '').split('&');
                            s.forEach(function(item) {
                                if (!item) return;
                                let kv = item.split('=');
                                params[decodeURIComponent(kv[0])] = decodeURIComponent(kv[1] || '');
                            });
                            return params;
                        }
                    
                        // --- Загрузка конфигурации VK с бэкенда ---
                        async function loadVkConfig() {
                            try {
                                const res = await fetch(API_BASE + "/oauth2/vk_id-config", {
                                    method: "GET",
                                    headers: { "Content-Type": "application/json" }
                                });
                                if (!res.ok) throw new Error("Failed to fetch VK config");
                                vkConfig = await res.json();
                                console.log("VK Config loaded:", vkConfig);
                            } catch (err) {
                                console.error("Error loading VK config:", err);
                                showStatus("Ошибка загрузки настроек VK. Попробуйте позже.", "red");
                            }
                        }
                    
                        // --- Инициализация при загрузке страницы ---
                        document.addEventListener("DOMContentLoaded", async function () {
                            // Загружаем конфиг VK сразу
                            await loadVkConfig();
                    
                            const params = getQueryParams();
                    
                            // Обработка ответа от Google / Яндекс OAuth
                            if (params.auth_status) {
                                let userMsg = "";
                                let color = ["error", "ERROR"].includes(params.auth_status) ? "red" : "green";
                                const engStatus = params.auth_status.toUpperCase();
                    
                                if (socialAuthStatusLabels[engStatus]) {
                                    userMsg += `<strong>Статус:</strong> <b>${engStatus}</b><br>`;
                                    userMsg += `<span>${socialAuthStatusLabels[engStatus]}</span>`;
                                } else {
                                    userMsg += `<strong>${engStatus}</strong>`;
                                }
                                if (params.baseUserEmail) {
                                    userMsg += `<br><strong>Email:</strong> <span style="color:#294074;">${params.baseUserEmail}</span>`;
                                }
                                if (params.message) {
                                    userMsg += `<br><span>${params.message}</span>`;
                                }
                    
                                // Если есть auth_code — показываем выбор действия
                                if (params.auth_code && engStatus !== "ERROR") {
                                    userMsg += `<form class="social-actions-form" id="exchange-form">`;
                                    userMsg += `<div style="margin-bottom:6px;font-size:15px;">Выберите действие:</div>`;
                                    ["REGISTER", "LOGIN", "LINK"].forEach((el, i) => {
                                        userMsg += `<label class="radio-option">
                                            <input type="radio" name="exchange-action" value="${el}" ${i === 0 ? "checked" : ""}>
                                            ${oauthRequestStatusLabels[el] || el}
                                        </label>`;
                                    });
                                    userMsg += `<button type="submit" class="exchange-action-btn">Выполнить</button>`;
                                    userMsg += `</form>`;
                                }
                                showStatus(userMsg, color);
                    
                                // Обработка формы выбора действия
                                setTimeout(() => {
                                    const form = document.getElementById('exchange-form');
                                    if (form) {
                                        form.onsubmit = function (e) {
                                            e.preventDefault();
                                            const action = form.querySelector('input[name="exchange-action"]:checked');
                                            if (!action) {
                                                showStatus(userMsg + "<div style='color:red;'>Выберите действие</div>", "red");
                                                return;
                                            }
                                            socialExchangeRequest(params, action.value);
                                        };
                                    }
                                }, 100);
                            }
                    
                            // Обработка ответа от VK ID после редиректа
                            const vkParams = new URLSearchParams(window.location.search);
                            if (vkParams.has("code") && vkParams.has("state") && vkParams.has("device_id")) {
                                handleVkCallback(vkParams);
                            }
                        });
                    
                        // --- Обмен кода для Google / Яндекс ---
                        // --- Отправка обмена на нужный endpoint по action ---
                        function socialExchangeRequest(params, action) {
                            showStatus("Отправляем выбранное действие...", "#3858e9");
                            let endpoint = "/oauth2/social/exchange";
                            if (action === "REGISTER") endpoint = "/oauth2/social/register";
                            else if (action === "LOGIN") endpoint = "/oauth2/social/login";
                            else if (action === "LINK") endpoint = "/oauth2/social/link";
                            fetch(API + endpoint, {
                                method: "POST",
                                headers: { "Content-Type": "application/json" },
                                credentials: "include",
                                body: JSON.stringify({
                                    authCode: params.auth_code,
                                    provider: params.provider
                                })
                            })
                                .then(res => res.json())
                                .then(res => {
                                    if (res.accessToken) {
                                        localStorage.setItem('accessToken', res.accessToken);
                                        if (res.refreshToken) localStorage.setItem('refreshToken', res.refreshToken);
                                        showStatus(`<span style="color:#117a38;">Успешно!</span><br>${res.message || "Вход выполнен."}`, "green");
                                    } else {
                                        showStatus("Ошибка: " + (res.message || ""), "red");
                                    }
                                })
                                .catch(() => showStatus("Ошибка обмена кода!", "red"));
                        }
                    
                        // --- Вход по логину/паролю ---
                        document.getElementById('login-form').onsubmit = function (e) {
                            e.preventDefault();
                            showStatus("Выполняется вход...");
                            fetch(API + "/authentication/login", {
                                method: "POST",
                                headers: { "Content-Type": "application/json" },
                                credentials: "include",
                                body: JSON.stringify({
                                    email: this.email.value,
                                    password: this.pass.value
                                })
                            })
                                .then(r => r.json())
                                .then(data => {
                                    if (data.accessToken) {
                                        localStorage.setItem('accessToken', data.accessToken);
                                        if (data.refreshToken) localStorage.setItem('refreshToken', data.refreshToken);
                                        showStatus("Успешный вход!", "green");
                                    } else {
                                        showStatus(data.message || "Ошибка входа", "red");
                                    }
                                })
                                .catch(() => showStatus("Ошибка соединения с API", "red"));
                        };
                    
                        // --- Авторизация через Google и Яндекс ---
                        document.getElementById('google-login').onclick = () => {
                            window.location.href = API + "/oauth2/authorization/google";
                        };
                        document.getElementById('yandex-login').onclick = () => {
                            window.location.href = API + "/oauth2/authorization/yandex";
                        };
                    
                        // --- Генерация PKCE для VK ID ---
                        async function generatePKCE() {
                            const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';
                            const arr = Array.from(crypto.getRandomValues(new Uint8Array(64)));
                            const verifier = arr.map(x => chars.charAt(x % chars.length)).join('');
                            const data = new TextEncoder().encode(verifier);
                            const hashed = await crypto.subtle.digest("SHA-256", data);
                            const challenge = btoa(String.fromCharCode(...new Uint8Array(hashed)))
                                .replace(/\\+/g, '-').replace(/\\//g, '_').replace(/=+$/, '');
                    
                            return { verifier, challenge };
                        }
                    
                        function uuid() {
                            return ([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g, c =>
                                (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
                            );
                        }
                    
                        // --- Начало авторизации через VK ID ---
                        document.getElementById('vk-login').onclick = async function () {
                            if (!vkConfig) {
                                showStatus("Конфигурация VK ещё не загружена. Подождите...", "red");
                                return;
                            }
                    
                            const { verifier, challenge } = await generatePKCE();
                            const state = uuid();
                    
                            localStorage.setItem("vk_code_verifier", verifier);
                            localStorage.setItem("vk_auth_state", state);
                    
                            const params = new URLSearchParams({
                                response_type: "code",
                                client_id: vkConfig.clientId,
                                redirect_uri: vkConfig.redirectUri,
                                scope: vkConfig.scope,
                                state: state,
                                code_challenge: challenge,
                                code_challenge_method: "S256"
                            });
                    
                            window.location = "https://id.vk.ru/authorize?" + params.toString();
                        };
                    
                        // --- Обработка колбэка от VK ---
                        async function handleVkCallback(params) {
                            const gotState = params.get("state");
                            const origState = localStorage.getItem("vk_auth_state");
                    
                            if (gotState !== origState) {
                                showStatus("Ошибка: state mismatch!", "red");
                                return;
                            }
                    
                            const code = params.get("code");
                            const device_id = params.get("device_id");
                            const code_verifier = localStorage.getItem("vk_code_verifier");
                    
                            showStatus("Обработка данных от VK...");
                    
                            try {
                                const res = await fetch(API_BASE + "/oauth2/vk_pkce_token", {
                                    method: "POST",
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify({ code, device_id, code_verifier, state: gotState })
                                });
                    
                                const data = await res.json();
                    
                                // Ожидаем: { status, authCode, baseUserEmail, message }
                                if (!data.status || !data.authCode) {
                                    throw new Error("Invalid response from backend: missing status or authCode");
                                }
                    
                                const engStatus = data.status.toUpperCase();
                                let userMsg = `<strong>Статус:</strong> <b>${engStatus}</b><br>`;
                                userMsg += `<span>${socialAuthStatusLabels[engStatus] || data.message || "Неизвестный статус"}</span>`;
                    
                                if (data.baseUserEmail) {
                                    userMsg += `<br><strong>Email:</strong> <span style="color:#294074;">${data.baseUserEmail}</span>`;
                                }
                                if (data.message && !userMsg.includes(data.message)) {
                                    userMsg += `<br><span>${data.message}</span>`;
                                }
                    
                                // === Выбор действия если не ошибка ===
                                if (data.authCode && engStatus !== "ERROR") {
                                    userMsg += `<form class="social-actions-form" id="exchange-form">`;
                                    userMsg += `<div style="margin-bottom:6px;font-size:15px;">Выберите действие:</div>`;
                                    ["REGISTER", "LOGIN", "LINK"].forEach((action, i) => {
                                        userMsg += `<label class="radio-option">
                                    <input type="radio" name="exchange-action" value="${action}" ${i === 0 ? "checked" : ""}>
                                    ${oauthRequestStatusLabels[action] || action}
                                </label>`;
                                    });
                                    userMsg += `<button type="submit" class="exchange-action-btn">Выполнить</button>`;
                                    userMsg += `</form>`;
                                }
                    
                                showStatus(userMsg, engStatus === "ERROR" ? "red" : "green");
                    
                                // === Форма действия: вызываем socialExchangeRequest с правильным endpoint ===
                                setTimeout(() => {
                                    const form = document.getElementById('exchange-form');
                                    if (form) {
                                        form.onsubmit = function (e) {
                                            e.preventDefault();
                                            const actionInput = form.querySelector('input[name="exchange-action"]:checked');
                                            if (!actionInput) {
                                                showStatus(userMsg + "<div style='color:red;'>Выберите действие</div>", "red");
                                                return;
                                            }
                                            const action = actionInput.value;
                    
                                            // ---- ВНИМАНИЕ: отправляем на РАЗНЫЙ endpoint! ----
                                            socialExchangeRequest({
                                                auth_code: data.authCode,
                                                baseUserEmail: data.baseUserEmail,
                                                message: data.message,
                                                provider: "VK"
                                            }, action);
                                        };
                                    }
                                }, 100);
                    
                            } catch (err) {
                                console.error("Error during VK callback:", err);
                                showStatus("Ошибка при обмене кода с бэкендом: " + (err.message || ""), "red");
                            }
                        }
                    </script>
                    </body>
                    </html>
                    
                    ```
                    """)
    @GetMapping(value = "/vk/guide")
    private void guideVk() {/* Только для документирования */}


    @Operation(
            summary = "Вход через соцсеть",
            description = "Использует одноразовый код для входа существующего пользователя"
    )
    @PostMapping("/login")
    public ResponseEntity<?> socialLogin(@RequestBody SocialExchangeRequest req) {
        return handleSocialAction(req, null, OAuthRequestStatus.LOGIN);
    }

    @Operation(
            summary = "Регистрация через соцсеть",
            description = "Создаёт нового пользователя по данным из соцсети"
    )
    @PostMapping("/register")
    public ResponseEntity<?> socialRegister(@RequestBody SocialExchangeRequest req) {
        return handleSocialAction(req, null, OAuthRequestStatus.REGISTER);
    }

    @Operation(
            summary = "Привязка соцсети к аккаунту",
            description = "Привязывает соцсеть к уже авторизованному пользователю"
    )
    @PostMapping("/link")
    public ResponseEntity<?> socialLink(
            @RequestBody SocialExchangeRequest req,
            @AuthenticationPrincipal UserDetails currentUser) {
        return handleSocialAction(req, currentUser, OAuthRequestStatus.LINK);
    }

    // Вспомогательный метод
    private ResponseEntity<?> handleSocialAction(SocialExchangeRequest req, UserDetails currentUser, OAuthRequestStatus action) {
        String authCode = req.getAuthCode();
        OAuthProvider provider = req.getProvider();

        SocialUserInfo info = oneTimeAuthCodeService.consumeCode(authCode);
        if (info == null) {
            throw new SocialAuthException("Код устарел или неверен", 400);
        }

        User user;
        switch (action) {
            case LOGIN:
                user = userRepository.findBySocialId(info.getId(), provider)
                        .orElseThrow(() -> new SocialAuthException("Пользователь не найден", 404));
                return buildTokenResponse(user, "Успешный вход");
            case REGISTER:
                if (userRepository.findByEmail(info.getEmail()).isPresent()) {
                    throw new SocialAuthException("Email уже используется", 409);
                }
                user = registration.registerNewUser(provider, info);
                return buildTokenResponse(user, "Регистрация успешна");
            case LINK:
                if (currentUser == null) throw new SocialAuthException("Требуется авторизация", 401);
                user = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
                if (user == null) throw new SocialAuthException("Пользователь не найден", 403);
                linkingService.linkSocialAccount(user, info, provider);
                return buildTokenResponse(user, "Соцсеть привязана");
            default:
                throw new SocialAuthException("Неизвестное действие", 400);
        }
    }

    private ResponseEntity<?> buildTokenResponse(User user, String message) {
        String jwt = jwtService.generateToken(user);
        String refresh = refreshTokenService.createRefreshToken(user.getId()).getToken();

        Map<String, String> body = new HashMap<>();
        body.put("accessToken", jwt);
        body.put("refreshToken", refresh);
        body.put("email", user.getEmail());
        body.put("message", message);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtService.generateJwtCookie(jwt).toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenService.generateRefreshTokenCookie(refresh).toString())
                .body(body);
    }
}