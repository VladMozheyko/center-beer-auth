package fr.mossaab.security.service.social;

import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.exception.SocialAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthUserInfoService {

    private final OAuth2UserInfoExtractorFactory extractorFactory;

    public SocialUserInfo getUserInfo(OAuth2User oAuth2User, String accessToken, OAuthProvider provider) {
        SocialUserInfo userInfo;

        log.info("Получение данных пользователя для Provider: {}...", provider);
        if (oAuth2User != null) {
            OAuth2UserExtractor extractor = extractorFactory.getOAuth2UserExtractor(provider);
            userInfo = extractor.extract(oAuth2User);
        } else if (!accessToken.isBlank()) {
            AccessTokenExtractor extractor = extractorFactory.getAccessTokenExtractor(provider);
            userInfo = extractor.extract(accessToken);
        } else {
            log.error("Ошибка получения информации о пользователе");
            throw new IllegalArgumentException("No credentials provided for social login");
        }
        log.info("Данные получены успешно.");
        return userInfo;
    }
}
