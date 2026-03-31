package fr.mossaab.security.service.social;

import fr.mossaab.security.dto.social.SocialUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class VkOAuth2UserInfoExtractor implements AccessTokenExtractor {

    @Value("${vk.oauth2.client-id}")
    private String clientId;

    private final RestTemplate restTemplate;

    @Override
    public SocialUserInfo extract(String accessToken) {

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

            if (body == null) {
                throw new IllegalArgumentException("VK response is empty");
            }

            if (!body.containsKey("user")) {
                throw new IllegalArgumentException("VK response does not contain 'user': " + body);
            }

            Map<String, Object> user = (Map<String, Object>) body.get("user");

            // Обязательные поля
            Object userIdObj = user.get("user_id");
            if (userIdObj == null) {
                throw new IllegalArgumentException("Missing 'user_id' in VK response");
            }

            String userId = userIdObj.toString();
            String email = (String) user.get("email"); // может быть null
            String firstName = (String) user.get("first_name");
            String lastName = (String) user.get("last_name");

            return new SocialUserInfo(userId, email, firstName, lastName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user info from VK: " + e.getMessage(), e);
        }
    }
}
