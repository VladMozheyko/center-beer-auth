package fr.mossaab.security.service.social.extractor;

import fr.mossaab.security.dto.social.SocialUserInfo;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Извлекает данные пользователя из Google OAuth2.
 * Поддерживает поля: email, given_name, family_name, sub.
 */
@Component
public class GoogleUserInfoExtractor implements UserInfoExtractor {

    @Override
    public SocialUserInfo extract(OAuth2User oAuth2User, String accessToken) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String firstName = (String) attributes.get("given_name");
        String lastName = (String) attributes.get("family_name");
        String id = (String) attributes.get("sub");

        return new SocialUserInfo(id, email, firstName, lastName);
    }
}
