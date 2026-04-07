package fr.mossaab.security.service.social.factory;

import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.service.social.extractor.UserInfoExtractor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Фабрика для получения экстракторов информации о пользователе по провайдеру.
 * Регистрирует все бины UserInfoExtractor при старте.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserInfoExtractorFactory {

    private final List<UserInfoExtractor> extractors;
    private final Map<String, UserInfoExtractor> extractorMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        extractors.forEach(extractor -> {
            String name = inferProviderName(extractor.getClass().getSimpleName());
            extractorMap.put(name.toLowerCase(), extractor);
            log.debug("Registered extractor for provider: {}", name);
        });
    }

    private String inferProviderName(String className) {
        return className
                .replace("UserInfoExtractor", "")
                .replace("Extractor", "")
                .toLowerCase();
    }

    public UserInfoExtractor getExtractor(OAuthProvider provider) {
        return getExtractor(provider.name().toLowerCase());
    }

    public UserInfoExtractor getExtractor(String provider) {
        UserInfoExtractor extractor = extractorMap.get(provider.toLowerCase());
        if (extractor == null) {
            log.error("No extractor found for provider: {}", provider);
            throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        }
        return extractor;
    }
}
