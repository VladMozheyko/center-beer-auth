package fr.mossaab.security.backup.core.scheduler;

import fr.mossaab.security.backup.core.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Планировщик регулярных операций бэкапа.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final BackupService backupService;

    @Scheduled(cron = "#{@backupScheduleProperties.dailyBackupCron}",
            zone = "#{@backupScheduleProperties.zoneId}")
    public void runBackup() {

        try {
            log.info("[BACKUP_PIPELINE] Запуск ежедневного бэкапа");
            backupService.exportSystemBackup();
            log.info("[BACKUP_PIPELINE] Ежедневный бэкап успешно завершён");
        } catch (Exception e) {
            log.error("[BACKUP_PIPELINE] Ошибка при выполнении ежедневного бэкапа, этапы promotion/retention будут пропущены", e);
            // если бэкап не создался — дальнейшие шаги не выполняем
            return;
        }
    }
}