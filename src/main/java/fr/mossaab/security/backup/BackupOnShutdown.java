package fr.mossaab.security.backup;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * При штатном останове контейнера (Ctrl-C, docker stop, SigTERM) – снимаем дамп,
 * причём раньше, чем Hibernate успеет дропнуть schema (если стоит create-drop).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupOnShutdown {

    private final BackupService backupService;

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    public void onContextClosed(ContextClosedEvent e) {
        log.info("🛈 ContextClosedEvent – making snapshot …");
        backupService.save();
    }
}

