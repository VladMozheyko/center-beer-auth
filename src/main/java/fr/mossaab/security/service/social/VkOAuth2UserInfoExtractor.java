package fr.mossaab.security.service.social;

import fr.mossaab.security.dto.social.SocialUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class VkOAuth2UserInfoExtractor implements OAuth2UserInfoExtractor {

    @Value("${vk.oauth2.client-id}")
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

            if (body != null && body.containsKey("user")) {
                Map<String, Object> userInfo = (Map<String, Object>) body.get("user");

                String email = (String) userInfo.get("email");
                String firstName = (String) userInfo.get("first_name");
                String lastName = (String) userInfo.get("last_name");
                String userId = userInfo.get("user_id").toString();

                return new SocialUserInfo(userId, email, firstName, lastName);
            }
        } catch (RestClientException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
