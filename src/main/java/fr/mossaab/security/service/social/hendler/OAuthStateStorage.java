package fr.mossaab.security.service.social.hendler;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Хранилище для OAuth2 state параметров с использованием Caffeine Cache.
 * Позволяет проверять state параметр в stateless-режиме без сессий.
 */
@Component
public class OAuthStateStorage {

    private final Cache<String, String> cache = Caffeine.newBuilder()
            .expireAfterWrite(120, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();

    /**
     * Сохраняет соответствие между springState и customState.
     *
     * @param springState  state, сгенерированный Spring Security
     * @param customState  кастомный state (например, для хранения информации о редиректе)
     */
    public void put(String springState, String customState) {
        cache.put(springState, customState);
    }

    /**
     * Получает customState по springState.
     *
     * @param springState state, сгенерированный Spring Security
     * @return customState или null если ключ не найден
     */
    public String get(String springState) {
        return cache.getIfPresent(springState);
    }

    /**
     * Удаляет state из кэша.
     *
     * @param springState state, который нужно удалить
     */
    public void remove(String springState) {
        cache.invalidate(springState);
    }

    /**
     * Проверяет, существует ли state в кэше.
     *
     * @param springState state для проверки
     * @return true если state найден, иначе false
     */
    public boolean contains(String springState) {
        return cache.asMap().containsKey(springState);
    }
}
