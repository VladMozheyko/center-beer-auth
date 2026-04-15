package fr.mossaab.security.service.social.service;

import fr.mossaab.security.dto.social.SocialUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для выдачи и одноразового использования временных кодов аутентификации.
 * <p>
 * Используется для безопасной передачи данных пользователя (например, из OAuth2-колбэка)
 * между этапами авторизации, особенно при редиректе на фронтенд.
 * </p>
 * <p>
 * Коды живут 3 минуты, после чего становятся недействительными.
 * Очистка устаревших кодов выполняется автоматически каждые 5 минут.
 * </p>
 */
@Service
@Slf4j
public class OneTimeAuthCodeService {

    private final Map<String, AuthCodeInfo> codeMap = new ConcurrentHashMap<>();

    /**
     * Выдаёт новый одноразовый код, связанный с данными пользователя.
     *
     * @param socialUserInfo информация о пользователе из соцсети
     * @return уникальный строковый код (UUID)
     */
    public String issueCode(SocialUserInfo socialUserInfo) {
        log.info("[User Info] - Сохранение данных о пользователе email:{} во временное хранилище", socialUserInfo.getEmail());
        String code = UUID.randomUUID().toString();
        codeMap.put(code, new AuthCodeInfo(socialUserInfo, Instant.now().plusSeconds(180))); // 3 мин
        log.info("[User Info] - Временные данные сохранены");
        return code;
    }

    /**
     * Использует код: возвращает данные пользователя и удаляет код.
     * Код становится недействительным после первого вызова или по истечении времени.
     *
     * @param code одноразовый код
     * @return SocialUserInfo, если код валиден; иначе null
     */
    public SocialUserInfo consumeCode(String code) {
        log.info("[User Info] - Получение данных о пользователе oAuth из временного хранилища по коду:{}", code);
        AuthCodeInfo info = codeMap.remove(code);
        if (info == null || Instant.now().isAfter(info.expires)) {
            log.info("[User Info] - Данных по коду не найдено");
            return null;
        }
        log.info("[User Info] - Данные успешно получены");
        return info.socialUserInfo;
    }

    /**
     * Регулярная очистка просроченных кодов (раз в 5 минут).
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void cleanUpExpiredCodes() {
        Instant now = Instant.now();
        int count = 0;
        Iterator<Map.Entry<String, AuthCodeInfo>> it = codeMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, AuthCodeInfo> entry = it.next();
            if (now.isAfter(entry.getValue().expires)) {
                it.remove();
                count++;
            }
        }
        if (count > 0) {
            log.info("Cleaned up {} expired auth codes", count);
        }
    }

    private static class AuthCodeInfo {
        final SocialUserInfo socialUserInfo;
        final Instant expires;

        AuthCodeInfo(SocialUserInfo userInfo, Instant expires) {
            this.socialUserInfo = userInfo;
            this.expires = expires;
        }
    }
}