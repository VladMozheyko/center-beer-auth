package fr.mossaab.security.service.social;

import fr.mossaab.security.dto.social.SocialUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OneTimeAuthCodeService {
    private final Map<String, AuthCodeInfo> codeMap = new ConcurrentHashMap<>();

    public String issueCode(SocialUserInfo socialUserInfo) {
        String code = UUID.randomUUID().toString();
        codeMap.put(code, new AuthCodeInfo(socialUserInfo, Instant.now().plusSeconds(180))); // 3 мин
        return code;
    }

    public SocialUserInfo consumeCode(String code) {
        AuthCodeInfo info = codeMap.remove(code);
        if (info == null) return null;
        if (Instant.now().isAfter(info.expires)) return null;
        return info.socialUserInfo;
    }

    // Служебная задача: чистка по расписанию (раз в 5 минут)
    @Scheduled(fixedDelay = 5 * 60 * 1000) // каждые 5 минут
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
            log.info("Cleaned up expired codes in {} seconds", count);
        }
    }

    private static class AuthCodeInfo {
        SocialUserInfo socialUserInfo;
        Instant expires;

        AuthCodeInfo(SocialUserInfo userInfo, Instant expires) {
            this.socialUserInfo = userInfo;
            this.expires = expires;
        }
    }
}
