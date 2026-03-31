package fr.mossaab.security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/oauth2/vk_id-config")
@RequiredArgsConstructor
@Tag(name = "OAuth2", description = "Вход/регистрация/линк через соцсети2")
public class VkIdConfigController {

    @Value("${vk.oauth2.client-id}")
    private String vkClientId;

    @Value("${vk.oauth2.scope}")
    private String scope;

    @Value("${vk.oauth2.redirect-uri}")
    private String redirectUri;

    @Value("${vk.oauth2.authUrl}")
    private String authBackendUrl;

    @Operation(summary = "Служебный контроллер выдачи данных для вк", description = "Выдает настройки для запроса в VK используя OneTap (vkClientId, scope, redirectUri)")
    @GetMapping
    public VkIdConfigResponse getVkIdConfig() {
        return new VkIdConfigResponse(vkClientId, scope, redirectUri, authBackendUrl);
    }

    @Data
    @AllArgsConstructor
    public static class VkIdConfigResponse {
        private String clientId;
        private String scope;
        private String redirectUri;
        private String authBackendUrl;
    }
}
