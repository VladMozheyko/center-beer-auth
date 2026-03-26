package fr.mossaab.security.service.social;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2UserInfoExtractorFactory {

    private final Map<String, OAuth2UserInfoExtractor> extractors = new HashMap<>();

    // Автоматически собирает все бины-экстракторы
    public OAuth2UserInfoExtractorFactory(List<OAuth2UserInfoExtractor> extractorList) {
        for (OAuth2UserInfoExtractor extractor : extractorList) {
            String name = extractor.getClass().getSimpleName().replace("OAuth2UserInfoExtractor", "").toLowerCase();
            extractors.put(name, extractor);
        }
    }

    public OAuth2UserInfoExtractor getExtractor(String provider) {
        OAuth2UserInfoExtractor extractor = extractors.get(provider.toLowerCase());
        if (extractor == null) {
            throw new IllegalArgumentException("No extractor found for provider: " + provider);
        }
        return extractor;
    }
}
