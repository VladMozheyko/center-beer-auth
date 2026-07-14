package fr.mossaab.security.service.social.hendler;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OAuthStateStorage {
    private final Cache<String, String> cache = Caffeine.newBuilder()
            .expireAfterWrite(120, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();

    public void put(String springState, String customState) {
        cache.put(springState, customState);
    }

    public String get(String springState) {
        return cache.getIfPresent(springState);
    }

    public void remove(String springState) {
        cache.invalidate(springState);
    }
}


