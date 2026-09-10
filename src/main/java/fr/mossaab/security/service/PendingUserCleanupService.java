package fr.mossaab.security.service;

import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.UserStatus;
import fr.mossaab.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для очистки неактивированных (просроченных) аккаунтов.
 * 
 * Работает по двум сценариям:
 * 1. Каждые 24 часа: помечает PENDING аккаунты как EXPIRED
 * 2. Каждый день в 3:00: удаляет EXPIRED аккаунты старше 30 дней
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PendingUserCleanupService {

    private final UserRepository userRepository;

    /**
     * Помечает неактивированные аккаунты старше 24 часов как EXPIRED.
     * Выполняется каждые 24 часа.
     */
    @Scheduled(fixedRate = 86400000) // 24 часа
    @Transactional
    public void markExpiredPendingUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<User> pendingUsers = userRepository.findByStatusAndCreatedAtBefore(UserStatus.PENDING, cutoff);
        
        if (pendingUsers.isEmpty()) {
            log.debug("Нет неактивированных пользователей для пометки как EXPIRED");
            return;
        }

        int updatedCount = 0;
        for (User user : pendingUsers) {
            user.setStatus(UserStatus.EXPIRED);
            userRepository.save(user);
            updatedCount++;
        }
        
        log.info("Помечено {} неактивированных пользователей как EXPIRED (старше 24 часов)", updatedCount);
    }

    /**
     * Удаляет просроченные аккаунты старше 30 дней.
     * Выполняется каждый день в 3:00 ночи.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void deleteExpiredUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = userRepository.deleteByStatusAndCreatedAtBefore(UserStatus.EXPIRED, cutoff);
        
        if (deleted > 0) {
            log.info("Удалено {} просроченных пользователей старше 30 дней", deleted);
        } else {
            log.debug("Нет просроченных пользователей для удаления");
        }
    }
}
