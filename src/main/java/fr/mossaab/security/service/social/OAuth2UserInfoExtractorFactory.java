package fr.mossaab.security.service.social;

import fr.mossaab.security.enums.OAuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserInfoExtractorFactory {

    private final List<OAuth2UserExtractor> oAuth2UserExtractorList;
    private final List<AccessTokenExtractor> accessTokenExtractorList;

    private final Map<String, OAuth2UserExtractor> oAuth2UserExtractors = new ConcurrentHashMap<>();
    private final Map<String, AccessTokenExtractor> accessTokenExtractors = new ConcurrentHashMap<>();

    @jakarta.annotation.PostConstruct
    public void init() {
        // Загружаем все бины после создания
        for (OAuth2UserExtractor extractor : oAuth2UserExtractorList) {
            String name = getExtractorName(extractor);
            oAuth2UserExtractors.put(name, extractor);
        }

        for (AccessTokenExtractor extractor : accessTokenExtractorList) {
            String name = getExtractorName(extractor);
            accessTokenExtractors.put(name, extractor);
        }
    }

    private String getExtractorName(Object extractor) {
        String className = extractor.getClass().getSimpleName();
        return className.replace("OAuth2UserInfoExtractor", "").replace("Extractor", "").toLowerCase();
    }

    public OAuth2UserExtractor getOAuth2UserExtractor(OAuthProvider provider) {
        return getOAuth2UserExtractor(provider.name().toLowerCase());
    }

    public OAuth2UserExtractor getOAuth2UserExtractor(String provider) {
        OAuth2UserExtractor extractor = oAuth2UserExtractors.get(provider.toLowerCase());
        log.debug("provider {} -> extractor {}", provider.toUpperCase(), extractor);
        if (extractor == null) {
            log.error("Ошибка получения экстрактора для провайдера {}", provider);
            throw new IllegalArgumentException("No OAuth2UserExtractor found for provider: " + provider);
        }
        return extractor;
    }

    public AccessTokenExtractor getAccessTokenExtractor(OAuthProvider provider) {
        return getAccessTokenExtractor(provider.name().toLowerCase());
    }

    public AccessTokenExtractor getAccessTokenExtractor(String provider) {
        AccessTokenExtractor extractor = accessTokenExtractors.get(provider.toLowerCase());
        log.debug("provider {} -> extractor {}", provider.toUpperCase(), extractor);
        if (extractor == null) {
            log.error("Ошибка получения экстрактора для провайдера {}", provider);
            throw new IllegalArgumentException("No AccessTokenExtractor found for provider: " + provider);
        }
        return extractor;
    }
}
