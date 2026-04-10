package fr.mossaab.security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для получения конфигурации VK ID OAuth2.
 * Используется фронтендом для инициализации входа через VK (например, OneTap).
 */
@RestController
@RequestMapping("/oauth2/vk_id-config")
@RequiredArgsConstructor
@Tag(name = "OAuth2", description = "Вход/регистрация/линк через соцсети2")
public class VkIdConfigController {

    @Value("${spring.security.oauth2.client.registration.vk.client-id}")
    private String vkClientId;

    @Value("${spring.security.oauth2.client.registration.vk.scope}")
    private String scope;

    @Value("${spring.security.oauth2.client.registration.vk.redirect-uri}")
    private String redirectUri;

    @Value("${app.server.base-url}")
    private String authBackendUrl;

    /**
     * Возвращает базовые настройки для OAuth2-авторизации через VK.
     * Фронтенд использует эти данные для запуска аутентификации.
     */
    @Operation(
            summary = "Получить конфигурацию VK ID",
            description = "Возвращает clientId, scope, redirectUri и backend auth URL для настройки VK OneTap или OAuth2",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Конфигурация VK ID успешно получена",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = VkIdConfigResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "clientId": "1234567",
                                                      "scope": "email profile",
                                                      "redirectUri": "https://myapp.com/auth/vk/callback",
                                                      "authBackendUrl": "https://api.myapp.com/api/v1/auth/vk"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    @GetMapping
    public VkIdConfigResponse getVkIdConfig() {
        String scopeString = scope.replace(",", " ");
        return new VkIdConfigResponse(vkClientId, scopeString, redirectUri, authBackendUrl);
    }

    @Data
    @AllArgsConstructor
    @Schema(description = "Конфигурация VK ID для фронтенда")
    public static class VkIdConfigResponse {

        @Schema(description = "Client ID приложения VK", example = "1234567")
        private String clientId;

        @Schema(description = "Запрашиваемые права (через пробел)", example = "email profile")
        private String scope;

        @Schema(description = "URI редиректа, на который VK отправит код авторизации",
                example = "https://myapp.com/auth/vk/callback")
        private String redirectUri;

        @Schema(description = "URL бэкенда для обмена кода авторизации на токен",
                example = "https://api.myapp.com/api/v1/auth/vk")
        private String authBackendUrl;
    }
}
