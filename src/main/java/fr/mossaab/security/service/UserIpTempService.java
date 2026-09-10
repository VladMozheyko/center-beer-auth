package fr.mossaab.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.dto.UserIpTempDto;
import fr.mossaab.security.entities.UserIpTemp;
import fr.mossaab.security.helper.IpHelper;
import fr.mossaab.security.repository.UserIpTempRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserIpTempService {

    private static final int MAX_IPS_TO_KEEP = 5;

    private final UserIpTempRepository repository;
    private final ObjectMapper objectMapper;
    private final IpHelper ipHelper;

    /**
     * Сохраняет IP-адрес пользователя во временную таблицу.
     * Хранит только последние 5 IP-адресов. Если их больше 5, удаляет самый старый.
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

        // Удаляем самый старый IP, если их уже 5
        List<UserIpTemp> existingIps = repository.findAllByUserIdOrderByCreatedAtDesc(userId);
        if (existingIps.size() >= MAX_IPS_TO_KEEP) {
            UserIpTemp oldestIp = existingIps.get(existingIps.size() - 1);
            repository.delete(oldestIp);
            log.debug("Удален самый старый IP {} для userId={}, так как лимит ({}) исчерпан", 
                    oldestIp.getIpAddress(), userId, MAX_IPS_TO_KEEP);
        }

        // Создаем новую запись
        UserIpTemp newEntry = new UserIpTemp();
        newEntry.setUserId(userId);
        newEntry.setIpAddress(ipAddress);
        newEntry.setIsPrivateOrLoopback(isPrivateOrLoopback);
        newEntry.setCreatedAt(Instant.now());

        repository.save(newEntry);
        log.debug("IP {} успешно сохранён для userId={}", ipAddress, userId);
    }

    /**
     * Возвращает список IP-адресов пользователя (не более 5), упорядоченных по времени создания (от новых к старым).
     */
    @Transactional(readOnly = true)
    public List<UserIpTempDto> getTrackedIpForUser(Long userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(MAX_IPS_TO_KEEP)
                .map(ip -> objectMapper.convertValue(ip, UserIpTempDto.class))
                .toList();
    }
}
