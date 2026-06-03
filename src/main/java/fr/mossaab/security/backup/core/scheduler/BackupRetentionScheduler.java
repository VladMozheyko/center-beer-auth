package fr.mossaab.security.backup.core.scheduler;

import fr.mossaab.security.backup.core.enums.BackupTier;
import fr.mossaab.security.backup.core.service.impl.BackupRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * Планировщик применения политики хранения (retention) для всех уровней бэкапов.
 * <p>
 * По расписанию, заданному в {@code backup.schedule.retention-cron},
 * обходит все значения {@link BackupTier} и для каждого уровня
 * запускает {@link BackupRetentionService#applyRetention(BackupTier)}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupRetentionScheduler {

    private final BackupRetentionService backupRetentionService;

    /**
     * Периодически применяет политику хранения ко всем уровням бэкапов.
     * <p>
     * CRON-выражение и часовой пояс берутся из {@code BackupScheduleProperties}
     * через SpEL-ссылки в параметрах аннотации {@link Scheduled}.
     */
    @Scheduled(cron = "#{@backupScheduleProperties.retentionCron}",
            zone = "#{@backupScheduleProperties.zoneId}")
    public void applyRetentionForAllTiers() {
        log.info("[BACKUP_RETENTION_SCHEDULER] запуск применения политики хранения для всех уровней");
        for (var tier : BackupTier.values()) {
            try {
                backupRetentionService.applyRetention(tier);
            } catch (Exception e) {
                log.error("[BACKUP_RETENTION_SCHEDULER] ошибка при применении политики для tier={}", tier, e);
            }
        }
    }
}