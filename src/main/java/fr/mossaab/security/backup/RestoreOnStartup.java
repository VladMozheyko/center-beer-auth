package fr.mossaab.security.backup;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Самый первый стартовый бин: если есть файл backup.json – восстанавливаемся.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestoreOnStartup implements SmartLifecycle {

    private final BackupService backupService;
    private boolean running = false;

    @Override public int getPhase()        { return Integer.MIN_VALUE; }  // раньше всех
    @Override public boolean isRunning()   { return running; }
    @Override public void stop()           { /* no-op */ }

    @Override
    public void start() {
        backupService.restoreIfExists();
        running = true;
    }
}
