package fr.mossaab.security.logger;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuditLogger {
    /**
     * Лог действия с IP (рекомендуемый для контроллеров)
     */
    public void logActionWithIP(ActionType action, String provider, String principal, String ip, String message) {
        String tmpl = "[{}] provider={}, principal={}, ip={}, msg={}";
        switch (action) {
            case LOGIN_FAIL, REGISTER_FAIL, LINK_FAIL ->
                    log.warn(tmpl, action, provider, safe(principal), safe(ip), message);
            case ERROR -> log.error(tmpl, action, provider, safe(principal), safe(ip), message);
            default -> log.info(tmpl, action, provider, safe(principal), safe(ip), message);
        }
    }

    /**
     * Лог действия без IP (например, для внутренних сервисов/событий)
     */
    public void logAction(ActionType action, String provider, String principal, String message) {
        String tmpl = "[{}] provider={}, principal={}, msg={}";
        switch (action) {
            case LOGIN_FAIL, REGISTER_FAIL, LINK_FAIL -> log.warn(tmpl, action, provider, safe(principal), message);
            case ERROR -> log.error(tmpl, action, provider, safe(principal), message);
            default -> log.info(tmpl, action, provider, safe(principal), message);
        }
    }

    /**
     * Лог внутренней ошибки с IP (рекомендуемый для всех try/catch ошибок)
     */
    public void logError(String message, String provider, String principal, String ip, Throwable ex) {
        log.error("[ERROR] {} provider={}, principal={}, ip={}, error={}", message, provider, safe(principal), safe(ip), ex.getMessage(), ex);
    }

    /**
     * Лог внутренней ошибки (без IP)
     */
    public void logError(String message, String provider, String principal, Throwable ex) {
        log.error("[ERROR] {} provider={}, principal={}, error={}", message, provider, safe(principal), ex.getMessage(), ex);
    }

    /**
     * Просто ошибка бизнес-потока (например, для fail-action)
     */
    public void logError(String message, String provider, String principal) {
        log.error("[ERROR] {} provider={}, principal={}", message, provider, safe(principal));
    }

    // helper — чтобы не было NullPointerException в логах
    private String safe(String val) {
        return val == null ? "-" : val;
    }

    public enum ActionType {
        LOGIN_ATTEMPT, LOGIN_SUCCESS, LOGIN_FAIL, REGISTER_ATTEMPT, REGISTER_SUCCESS, REGISTER_FAIL,
        LINK_ATTEMPT, LINK_SUCCESS, LINK_FAIL, ERROR, GET_CONFIG
    }
}