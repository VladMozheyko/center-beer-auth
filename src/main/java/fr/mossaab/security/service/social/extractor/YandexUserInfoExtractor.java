package fr.mossaab.security.service.social.extractor;

import fr.mossaab.security.dto.social.SocialUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Извлекает данные пользователя из Yandex OAuth2.
 * Использует поля: default_email, first_name, last_name, id.
 */
@Slf4j
@Component
public class YandexUserInfoExtractor implements UserInfoExtractor {

    @Override
    public SocialUserInfo extract(OAuth2User oAuth2User, String accessToken) {
        log.info("[YANDEX EXTRACTOR] - Извлечение информации о пользователе из данных аутентификации");

        Map<String, Object> attributes = oAuth2User.getAttributes();

        try {
            String email = (String) attributes.get("default_email");
            String firstName = (String) attributes.get("first_name");
            String lastName = (String) attributes.get("last_name");
            String id = (String) attributes.get("id");
            log.info("[YANDEX EXTRACTOR] - Данные успешно извлечены");
            return new SocialUserInfo(id, email, firstName, lastName);
        } catch (Exception e) {
            log.error("[YANDEX EXTRACTOR] - Ошибка при извлечении данных о пользователе из OAuth {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
