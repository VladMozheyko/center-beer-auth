package fr.mossaab.security.controller;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.service.social.OAuthUserInfoService;
import fr.mossaab.security.service.social.SocialUserFlowService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
@Slf4j
public class VkIdPKCEController {
    private final SocialUserFlowService flowService;
    private final OAuthUserInfoService userInfoService;

    @Value("${vk.oauth2.client-id}")
    private String vkClientId;
    @Value("${vk.oauth2.redirect-uri}")
    private String vkRedirectUri;

    @PostMapping("/vk_pkce_token")
    public ResponseEntity<?> vkPkceToken(@RequestBody Map<String, String> req) {
        String code = req.get("code");
        String device_id = req.get("device_id");
        String codeVerifier = req.get("code_verifier");
        String state = req.get("state");

        // 1. Получаем access_token по коду
        String accessToken = vkExchangeCodeForToken(code, codeVerifier, device_id);
        User user = new User();

        // 2. Используем access_token для получения UserInfo
        SocialUserInfo userInfo = userInfoService.getUserInfo(null, accessToken, OAuthProvider.VK);
        SocialUserFlowService.SocialAuthResult result = flowService.analyzeUser(userInfo, OAuthProvider.VK);

        return ResponseEntity.ok(result);
    }

    // метод обмена code на VK Access Token
    private String vkExchangeCodeForToken(String code, String codeVerifier, String deviceId) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://id.vk.ru/oauth2/auth";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", vkClientId);
        params.add("redirect_uri", vkRedirectUri);
        params.add("code", code);
        params.add("code_verifier", codeVerifier);
        params.add("device_id", deviceId);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, request, Map.class);
        Map body = resp.getBody();
        if (body == null || !body.containsKey("access_token"))
            throw new RuntimeException("VK ID не вернул access_token: " + body);
        return (String) body.get("access_token");
    }

    @GetMapping("/code/vk")
    public void vkRedirectProxy(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(name = "device_id", required = false) String deviceId,
            HttpServletResponse response
    ) throws IOException {
        // Собираем URL фронтенда с нужными params
        String frontendUrl = "http://localhost:8081/"
                + "?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8)
                + (deviceId != null ? "&device_id=" + URLEncoder.encode(deviceId, StandardCharsets.UTF_8) : "");
        response.sendRedirect(frontendUrl);
    }
}
