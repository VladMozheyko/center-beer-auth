package fr.mossaab.security.controller;

import io.swagger.v3.oas.annotations.Operation;
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
            description = "Возвращает clientId, scope, redirectUri и backend auth URL для настройки VK OneTap или OAuth2"
    )
    @GetMapping
    public VkIdConfigResponse getVkIdConfig() {
        String scopeString = scope.replace(",", " ");
        return new VkIdConfigResponse(vkClientId, scopeString, redirectUri, authBackendUrl);
    }

    @Data
    @AllArgsConstructor
    public static class VkIdConfigResponse {
        private String clientId;        // Client ID приложения VK
        private String scope;           // Запрашиваемые права (например, email)
        private String redirectUri;     // URI перехвата кода
        private String authBackendUrl;  // Адрес бэкенда для обмена кода на токен
    }
}
