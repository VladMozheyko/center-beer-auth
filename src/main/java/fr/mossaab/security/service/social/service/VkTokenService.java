package fr.mossaab.security.service.social.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Сервис для обмена authorization code на access token в VK ID с использованием PKCE.
 * Использует RestTemplate для отправки формы на VK OAuth2 endpoint.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VkTokenService {

    private final RestTemplate restTemplate;

    @Value("${frontend.server.address}")
    private String frontendUrl;

    @Value("${spring.security.oauth2.client.registration.vk.client-id}")
    private String vkClientId;

    @Value("${spring.security.oauth2.client.registration.vk.redirect-uri}")
    private String vkRedirectUri;

    /**
     * Обменивает authorization code на access token у VK.
     *
     * @param code         authorization code
     * @param codeVerifier PKCE code verifier
     * @param deviceId     идентификатор устройства (опционально)
     * @return access token
     */
    public String exchangeCodeForToken(String code, String codeVerifier, String deviceId) {
        log.info("[VK EXCHANGE CODE] - Обмен кода на токен в VK для device_id={}", deviceId);

        // URL для запроса токена
        String url = "https://id.vk.ru/oauth2/auth";

        // Заголовки: application/x-www-form-urlencoded
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Параметры формы
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", vkClientId);
        params.add("redirect_uri", vkRedirectUri);
        params.add("code", code);
        params.add("code_verifier", codeVerifier);
        if (deviceId != null && !deviceId.isBlank()) {
            params.add("device_id", deviceId);
        }

        // HTTP-запрос
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null) {
                log.warn("[VK EXCHANGE CODE] - Пустой ответ от VK при обмене кода на токен");
                throw new RuntimeException("VK вернул пустой ответ");
            }

            if (!body.containsKey("access_token")) {
                log.warn("[VK EXCHANGE CODE] - VK не вернул access_token: {}", body);
                throw new RuntimeException("VK не вернул access_token. Ответ: " + body);
            }

            log.info("[VK EXCHANGE CODE] - Получен access_token от VK");
            return (String) body.get("access_token");

        } catch (Exception e) {
            log.error("[VK EXCHANGE CODE] - Ошибка при обмене кода на токен с VK", e);
            throw new RuntimeException("Не удалось получить access_token от VK: " + e.getMessage(), e);
        }
    }

    /**
     * Формирует URL для редиректа на фронтенд с параметрами.
     */
    public String buildFrontendRedirectUrl(String code, String state, String deviceId) {
        StringBuilder url = new StringBuilder(frontendUrl)
                .append("?code=").append(URLEncoder.encode(code, StandardCharsets.UTF_8))
                .append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));

        if (deviceId != null && !deviceId.isBlank()) {
            url.append("&device_id=").append(URLEncoder.encode(deviceId, StandardCharsets.UTF_8));
        }

        return url.toString();
    }
}