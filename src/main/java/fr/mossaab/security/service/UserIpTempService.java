package fr.mossaab.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.dto.UserIpTempDto;
import fr.mossaab.security.entities.UserIpTemp;
import fr.mossaab.security.helper.IpHelper;
import fr.mossaab.security.repository.UserIpTempRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class UserIpTempService {

    private final UserIpTempRepository repository;
    private final ObjectMapper objectMapper;
    private final IpHelper ipHelper;

    @Value("${user.ip.ttl-seconds:300}")
    private long ttlSeconds;

    /**
     * Сохраняет IP-адрес пользователя во временную таблицу.
     * Всегда сохраняет IP, помечая его как приватный/loopback с помощью IpHelper.
     */
    @Transactional
    public void saveIpTemp(Long userId, String ipAddress) {
        // Валидация входных данных
        if (ipAddress == null || ipAddress.isBlank()) {
            log.warn("Не могу сохранить IP: адрес пустой или null для userId={}", userId);
            return;
        }

        boolean isPrivateOrLoopback = ipHelper.isInternalIp(ipAddress);
        
        // Всегда логируем IP-адрес
        log.info("IP-адрес {} для пользователя {} (приватный: {})", ipAddress, userId, isPrivateOrLoopback);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlSeconds);

        UserIpTemp newEntry = new UserIpTemp();
        newEntry.setUserId(userId);
        newEntry.setIpAddress(ipAddress);
        newEntry.setIsPrivateOrLoopback(isPrivateOrLoopback);
        newEntry.setCreatedAt(now);
        newEntry.setExpiresAt(expiresAt);

        repository.save(newEntry);
        log.debug("IP {} успешно сохранён для userId={}, expiresAt={}", ipAddress, userId, expiresAt);
    }

    /**
     * Возвращает список IP-адресов пользователя, упорядоченных по времени создания (от новых к старым).
     */
    @Transactional(readOnly = true)
    public List<UserIpTempDto> getTrackedIpForUser(Long userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ip -> objectMapper.convertValue(ip, UserIpTempDto.class))
                .toList();
    }

    /**
     * Удаляет устаревшие записи IP-адресов из базы данных.
     * Выполняется автоматически каждые 5 минут (настраивается через user.ip.cleanup-interval-ms).
     */
    @Transactional
    @Scheduled(fixedDelayString = "${user.ip.cleanup-interval-ms:300000}") // 5 минут по умолчанию
    public void cleanupExpired() {
        repository.deleteExpired(Instant.now());
    }
}
