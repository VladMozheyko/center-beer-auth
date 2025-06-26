package fr.mossaab.security.backup;


import fr.mossaab.security.service.UserCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Чисто демо-бин: при первом запуске создаёт админа, заполняет вопросники и сразу
 * делает бэкап.  Удалите, если не нужен.
 */
@Component
@RequiredArgsConstructor
public class DemoDataLoader implements ApplicationRunner {

    private final UserCreateService userCreateService;
    private final BackupService     backupService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        userCreateService.createUsers();
        backupService.save();
    }
}
