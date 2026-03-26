package fr.mossaab.security.service.social;

import fr.mossaab.security.dto.social.SocialUserInfo;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class YandexOAuth2UserInfoExtractor implements OAuth2UserInfoExtractor {

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
