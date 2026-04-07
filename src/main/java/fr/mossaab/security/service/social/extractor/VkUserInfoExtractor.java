package fr.mossaab.security.service.social.extractor;

import fr.mossaab.security.dto.social.SocialUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Извлекает данные пользователя из VK через API.
 * Требует access_token и client_id для запроса к /oauth2/user_info.
 */
@Component
@RequiredArgsConstructor
public class VkUserInfoExtractor implements UserInfoExtractor {

    @Value("${spring.security.oauth2.client.registration.vk.client-id}")
    private String clientId;

    private final RestTemplate restTemplate;

    @Override
    public SocialUserInfo extract(OAuth2User oAuth2User, String accessToken) {
        String url = "https://id.vk.ru/oauth2/user_info";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("access_token", accessToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null || !body.containsKey("user")) {
                throw new IllegalArgumentException("Invalid or empty response from VK: " + body);
            }

            Map<String, Object> user = (Map<String, Object>) body.get("user");

            String userId = String.valueOf(user.get("user_id"));
            String email = (String) user.get("email");
            String firstName = (String) user.get("first_name");
            String lastName = (String) user.get("last_name");

            return new SocialUserInfo(userId, email, firstName, lastName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user info from VK", e);
        }
    }
}
