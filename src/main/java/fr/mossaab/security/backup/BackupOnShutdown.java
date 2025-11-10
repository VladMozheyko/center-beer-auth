package fr.mossaab.security.backup;

import fr.mossaab.security.backup.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Делает бэкап при остановке приложения.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupOnShutdown {

    private final BackupService backupService;

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    public void onContextClosed(ContextClosedEvent e) {
        log.info("📴 Context is closing, running backup...");
        backupService.save();
    }
}
