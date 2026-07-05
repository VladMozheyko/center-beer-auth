package fr.mossaab.security.helper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.regex.Pattern;

/**
 * Класс для извлечения и валидации IP-адресов из HTTP-запросов.
 * Используется для получения реального IP-адреса клиента в прокси-среде.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IpHelper {

    private static final Pattern INTERNAL_IP = Pattern.compile(
            "^127\\.\\d+\\.\\d+\\.\\d+$|" +
                    "^10\\.\\d+\\.\\d+\\.\\d+$|" +
                    "^172\\.(1[6-9]|2\\d|3[01])\\.\\d+\\.\\d+$|" +
                    "^192\\.168\\.\\d+\\.\\d+$|" +
                    "^169\\.254\\.\\d+\\.\\d+$|" +
                    "^::1$|" +
                    "^(?:0+:)*:1$|" +
                    "^fd[0-9a-f:]+$"
    );

    /**
     * Извлекает реальный IP-адрес клиента из HTTP-запроса.
     * 
     * ПРИНЦИП: Если Nginx настроен правильно (удаляет заголовки X-Forwarded-For и X-Real-IP от клиента),
     * то заголовок X-Real-IP содержит доверенный IP клиента.
     * 
     * Приоритет:
     * 1. X-Real-IP — доверенный источник (если Nginx настроен правильно)
     * 2. X-Forwarded-For — если X-Real-IP не настроен (берем первый неприватный IP)
     * 3. getRemoteAddr() — fallback (для прямого подключения или локальной разработки)
     * 
     * @param request HTTP-запрос
     * @return реальный IP-адрес клиента
     */
    public String getClientIp(HttpServletRequest request) {
        // 1. X-Real-IP — доверенный источник (если Nginx настроен правильно)
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            log.debug("Извлечён доверенный IP из X-Real-IP: {}", realIp.trim());
            return realIp.trim();
        }

        // 2. X-Forwarded-For — если X-Real-IP не настроен
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] ips = xff.split(",");
            for (String ip : ips) {
                ip = ip.trim();
                if (!ip.isEmpty() && !isInternalIp(ip)) {
                    log.debug("Извлечён внешний IP из X-Forwarded-For: {}", ip);
                    return ip;
                }
            }
            // Если все IP внутренние, берем первый (реальный клиент в Docker)
            log.warn("Все IP из X-Forwarded-For внутренние: {}, берем первый: {}", xff, ips[0]);
            return ips[0].trim();
        }

        // 3. Fallback к remoteAddr
        String remoteAddr = request.getRemoteAddr();
        log.debug("Извлечён IP из getRemoteAddr (без прокси): {}", remoteAddr);
        return remoteAddr;
    }

    /**
     * Проверяет, является ли IP внутренним (loopback, приватный диапазон).
     * 
     * @param ip IP-адрес для проверки
     * @return true, если IP внутренний
     */
    public boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) return true;
        if (ip.startsWith("::ffff:")) {
            ip = ip.substring(7);
        }
        return INTERNAL_IP.matcher(ip).matches();
    }
}
