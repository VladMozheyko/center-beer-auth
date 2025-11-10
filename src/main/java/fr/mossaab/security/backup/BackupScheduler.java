package fr.mossaab.security.backup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Периодический бэкап.
 * CRON можно задать через:
 * backup.cron=0 0 * * * *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final BackupService backupService;

    @Scheduled(cron = "${backup.cron:0 0 * * * *}", zone = "Europe/Moscow")
    @Transactional(readOnly = true)
    public void backupHourly() {
        log.info("⏰ Scheduled backup started");
        backupService.save();
    }
}
