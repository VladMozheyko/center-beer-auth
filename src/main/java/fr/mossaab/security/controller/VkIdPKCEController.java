package fr.mossaab.security.controller;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.service.social.service.OAuthUserInfoService;
import fr.mossaab.security.service.social.service.SocialUserFlowService;
import fr.mossaab.security.service.social.service.VkTokenService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * Контроллер для обработки OAuth2-аутентификации через VK ID с использованием PKCE.
 * Поддерживает безопасный обмен authorization code на результат авторизации
 * и проксирование редиректа от VK к фронтенду.
 */
@Tag(name = "OAuth2", description = "API для аутентификации через VK ID с использованием механизма PKCE (Proof Key for Code Exchange). " +
        "Обеспечивает безопасную передачу данных между SPA-приложением и бэкендом без expose redirect_uri.")
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
@Slf4j
public class VkIdPKCEController {

    private final VkTokenService vkTokenService;
    private final OAuthUserInfoService userInfoService;
    private final SocialUserFlowService flowService;

    // ======================================
    // POST /oauth2/vk_pkce_token
    // ======================================

    @Operation(
            summary = "Обмен PKCE-кода на результат авторизации",
            description = """
                    Принимает authorization code и связанные с ним параметры от фронтенда, \
                    обменивает код на access token у VK ID, получает информацию о пользователе \
                    и возвращает анализ потока авторизации.

                    Используется на фронтенде после получения `code` от VK через редирект.
                    
                    <b>Типичный сценарий:</b>
                    <ol>
                      <li>Фронтенд запускает OAuth2-поток с генерацией code_challenge</li>
                      <li>Пользователь авторизуется в VK</li>
                      <li>VK перенаправляет на `/oauth2/code/vk` → прокси-редирект на фронт</li>
                      <li>Фронт отправляет `code` и `code_verifier` на этот endpoint</li>
                      <li>Бэкенд обменивает код на токен и возвращает статус пользователя</li>
                    </ol>
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для обмена кода на результат авторизации",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "object",
                                    example = """
                                            {
                                              "code": "abcd1234xyz",
                                              "code_verifier": "very_long_and_secure_verifier_string",
                                              "device_id": "mobile_001"
                                            }
                                            """
                            ),
                            examples = {
                                    @ExampleObject(
                                            name = "Успешный запрос",
                                            summary = "Стандартный запрос от мобильного приложения",
                                            value = """
                                                    {
                                                      "code": "auth_code_from_vk",
                                                      "code_verifier": "strong_pkce_verifier_abc123",
                                                      "device_id": "android_789"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Запрос без device_id",
                                            summary = "Для веб-приложений device_id может отсутствовать",
                                            value = """
                                                    {
                                                      "code": "web_code_5678",
                                                      "code_verifier": "web_verifier_xyz"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Успешный ответ с анализом потока пользователя",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = SocialUserFlowService.SocialAuthResult.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "status": "new_account",
                                                      "message": "Пользователь не найден. Можно продолжить регистрацию через соцсеть.",
                                                      "baseUserEmail": null,
                                                      "socialUser": {
                                                        "id": "123456789",
                                                        "email": "user@example.com",
                                                        "firstName": "Иван",
                                                        "lastName": "Иванов"
                                                      },
                                                      "authCode": "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Некорректные входные данные",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    value = "{\"error\":\"Missing 'code' or 'code_verifier'\"}"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Ошибка аутентификации у провайдера",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    { "error": "VK не вернул access_token" }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Внутренняя ошибка сервера",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    { "error": "Authentication failed: Connection timeout" }
                                                    """
                                    )
                            )
                    )
            },
            security = @SecurityRequirement(name = "none") // нет авторизации для этого endpoint
    )
    @PostMapping("/vk_pkce_token")
    public ResponseEntity<?> vkPkceToken(@RequestBody Map<String, String> req) {
        log.info("Обработка кода авторизации полученный после запроса PKCE");
        String code = req.get("code");
        String codeVerifier = req.get("code_verifier");
        String deviceId = req.get("device_id");

        if (code == null || codeVerifier == null) {
            log.error("[VK PKCE] - Отсутствует сервисный код авторизации (code) или код верификации от вк");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing 'code' or 'code_verifier'"));
        }

        try {
            String accessToken = vkTokenService.exchangeCodeForToken(code, codeVerifier, deviceId);
            SocialUserInfo userInfo = userInfoService.getUserInfo(null, accessToken, OAuthProvider.VK);
            SocialUserFlowService.SocialAuthResult result = flowService.analyzeUser(userInfo, OAuthProvider.VK);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[VK PKCE] - Ошибка при обработке PKCE-кода от VK", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    // ======================================
    // GET /oauth2/code/vk
    // ======================================

    @Operation(
            summary = "Прокси для редиректа от VK к фронтенду",
            hidden = true,// скрытая операция из документации API
            description = """
                    Перехватывает редирект от VK после успешной аутентификации \
                    и перенаправляет пользователя на фронтенд с сохранением параметров.

                    <b>Зачем это нужно?</b>
                    <ul>
                      <li>Не требуется exposing бэкендного redirect_uri наружу</li>
                      <li>Фронтенд остаётся единственным получателем authorization code</li>
                      <li>Улучшает безопасность SPA-приложений</li>
                    </ul>
                    
                    Параметры `code`, `state` и опционально `device_id` кодируются и передаются на фронтенд.
                    """,
            parameters = {
                    @Parameter(
                            name = "code",
                            description = "Authorization code, выданный VK",
                            required = true,
                            example = "abcd1234xyz",
                            schema = @Schema(type = "string")
                    ),
                    @Parameter(
                            name = "state",
                            description = "Случайная строка для защиты от CSRF-атак",
                            required = true,
                            example = "secure_random_state_5678",
                            schema = @Schema(type = "string")
                    ),
                    @Parameter(
                            name = "device_id",
                            description = "Идентификатор устройства (например, mobile, desktop)",
                            required = false,
                            example = "mobile_001",
                            schema = @Schema(type = "string")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "302",
                            description = "Успешное перенаправление на фронтенд",
                            content = @Content(
                                    mediaType = "text/html",
                                    examples = @ExampleObject(
                                            summary = "Redirect to frontend",
                                            value = "Location: http://localhost:8081/?code=...&state=..."
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Ошибка при перенаправлении",
                            content = @Content(
                                    mediaType = "text/plain",
                                    examples = @ExampleObject(value = "Internal Server Error")
                            )
                    )
            }
    )
    @GetMapping("/code/vk")
    public void vkRedirectProxy(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(name = "device_id", required = false) String deviceId,
            HttpServletResponse response
    ) throws IOException {
        try {
            log.info("Перенаправление кода авторизации и других данных на фронтенд");
            String redirectUrl = vkTokenService.buildFrontendRedirectUrl(code, state, deviceId);
            response.sendRedirect(redirectUrl);
        } catch (IOException e) {
            log.error("Ошибка при перенаправлении на фронтенд", e);
            throw e;
        }
    }


    @Data
    @Schema(description = "Запрос обмена VK PKCE-кода на токен")
    public class VkPkceTokenRequest {

        @Schema(description = "Код авторизации VK, полученный после редиректа", example = "AQAAABCD1234...")
        @NotBlank(message = "Поле 'code' обязательно")
        private String code;

        @Schema(description = "PKCE code_verifier, сгенерированный на клиенте", example = "s0m3L0ngR@nd0mStr1ng")
        @NotBlank(message = "Поле 'code_verifier' обязательно")
        private String codeVerifier;

        @Schema(description = "Идентификатор устройства (опционально)", example = "device-12345")
        private String deviceId;
    }
}