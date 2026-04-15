package fr.mossaab.security.service.social.extractor;

import fr.mossaab.security.dto.social.SocialUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Извлекает данные пользователя из Google OAuth2.
 * Поддерживает поля: email, given_name, family_name, sub.
 */
@Slf4j
@Component
public class GoogleUserInfoExtractor implements UserInfoExtractor {

    @Override
    public SocialUserInfo extract(OAuth2User oAuth2User, String accessToken) {
        log.info("[Google EXTRACTOR] - Извлечение информации о пользователе из данных аутентификации");
        Map<String, Object> attributes = oAuth2User.getAttributes();

        try {
            String email = (String) attributes.get("email");
            String firstName = (String) attributes.get("given_name");
            String lastName = (String) attributes.get("family_name");
            String id = (String) attributes.get("sub");
            log.info("[Google EXTRACTOR] - Данные успешно извлечены");
            return new SocialUserInfo(id, email, firstName, lastName);
        } catch (Exception e) {
            log.error("[Google EXTRACTOR] - Ошибка при извлечении данных о пользователе из OAuth {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
