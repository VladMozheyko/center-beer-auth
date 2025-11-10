/*
package fr.mossaab.security.backup;


import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.UserCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DemoDataLoader implements ApplicationRunner {

    private final UserCreateService userCreateService;
    private final UserRepository userRepo;
    private final BackupService backupService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (backupService.isAlreadyRestored()) return;
        if (!userRepo.existsByEmail("Vlad72229@yandex.ru")) {
            userCreateService.createUsers();
        }
        backupService.save();
    }
}*/
