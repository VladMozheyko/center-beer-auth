package fr.mossaab.security.service.social.extractor;

import fr.mossaab.security.dto.social.SocialUserInfo;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Извлекает данные пользователя из Yandex OAuth2.
 * Использует поля: default_email, first_name, last_name, id.
 */
@Component
public class YandexUserInfoExtractor implements UserInfoExtractor {

    @Override
    public SocialUserInfo extract(OAuth2User oAuth2User, String accessToken) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("default_email");
        String firstName = (String) attributes.get("first_name");
        String lastName = (String) attributes.get("last_name");
        String id = (String) attributes.get("id");

        return new SocialUserInfo(id, email, firstName, lastName);
    }
}
