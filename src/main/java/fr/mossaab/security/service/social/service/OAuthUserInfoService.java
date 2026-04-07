package fr.mossaab.security.service.social.service;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.service.social.extractor.UserInfoExtractor;
import fr.mossaab.security.service.social.factory.UserInfoExtractorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Основной сервис для получения информации о пользователе из OAuth2-провайдера.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthUserInfoService {

    private final UserInfoExtractorFactory extractorFactory;

    /**
     * Получает информацию о пользователе.
     *
     * @param oAuth2User  Данные из Spring Security (если доступны)
     * @param accessToken Access token (для провайдеров вроде GOOGLE, YANDEX, VK....)
     * @param provider    OAuth провайдер
     * @return SocialUserInfo
     */
    public SocialUserInfo getUserInfo(OAuth2User oAuth2User, String accessToken, OAuthProvider provider) {
        log.info("Fetching user info for provider: {}", provider);

        UserInfoExtractor extractor = extractorFactory.getExtractor(provider);
        SocialUserInfo userInfo = extractor.extract(oAuth2User, accessToken);

        log.info("User info retrieved successfully: id={}, email={}", userInfo.getId(), userInfo.getEmail());
        return userInfo;
    }
}
