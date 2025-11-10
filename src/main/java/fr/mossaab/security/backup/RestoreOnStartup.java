/*
package fr.mossaab.security.backup;

import fr.mossaab.security.backup.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

*/
/**
 * Автоматическое восстановление из backup.json при старте,
 * запускается одним из самых первых.
 *//*

@Slf4j
@Component
@RequiredArgsConstructor
public class RestoreOnStartup implements SmartLifecycle {

    private final BackupService backupService;
    private boolean running = false;

    @Override
    public int getPhase() {
        // Минимальная фаза — запускаем максимально рано
        return Integer.MIN_VALUE;
    }

    @Override
    public void start() {
        log.info("🔄 Trying to restore from backup on startup...");
        backupService.restoreIfExists();
        running = true;
    }

    @Override
    public void stop() {
        // ничего
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
*/
