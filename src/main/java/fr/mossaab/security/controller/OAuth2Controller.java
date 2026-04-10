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
import jakarta.validation.Valid;
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
                    
                    > Полный пример реализации доступен в 
                    > <a href="/vk-auth-example.html" target="_blank">vk-auth-example.html</a>
                    """)
    @GetMapping(value = "/vk/guide")
    private void guideVk() {/* Только для документирования */}


    @Operation(
            summary = "Вход через соцсеть",
            description = "Использует одноразовый код для входа существующего пользователя"
    )
    @PostMapping("/login")
    public ResponseEntity<?> socialLogin(@Valid @RequestBody SocialExchangeRequest req) {
        return handleSocialAction(req, null, OAuthRequestStatus.LOGIN);
    }

    @Operation(
            summary = "Регистрация через соцсеть",
            description = "Создаёт нового пользователя по данным из соцсети"
    )
    @PostMapping("/register")
    public ResponseEntity<?> socialRegister(@Valid @RequestBody SocialExchangeRequest req) {
        return handleSocialAction(req, null, OAuthRequestStatus.REGISTER);
    }

    @Operation(
            summary = "Привязка соцсети к аккаунту",
            description = "Привязывает соцсеть к уже авторизованному пользователю"
    )
    @PostMapping("/link")
    public ResponseEntity<?> socialLink(
            @RequestBody @Valid SocialExchangeRequest req,
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