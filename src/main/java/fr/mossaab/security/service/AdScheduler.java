package fr.mossaab.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Планировщик для регулярной проверки состояния рекламного лидерства.
 */
@Component
@RequiredArgsConstructor
public class AdScheduler {

    private final AdvertisementQueueService advertisementQueueService;

    /**
     * Проверяет и обновляет лидера рекламы каждые 60 секунд.
     * Если необходимо — назначает нового или сокращает время текущего.
     */
    @Scheduled(fixedRate = 60000)
    public void updateAdLeadership() {
        advertisementQueueService.updateLeadership();
    }
}
