package fr.mossaab.security.service.social.extractor;

import fr.mossaab.security.dto.social.SocialUserInfo;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface UserInfoExtractor {

    /**
     * Извлекает информацию о пользователе.
     *
     * @param oAuth2User  OAuth2User из Spring Security (может быть null)
     * @param accessToken Access token (может быть null)
     * @return SocialUserInfo или выбрасывает исключение
     */
    SocialUserInfo extract(OAuth2User oAuth2User, String accessToken);
}
