package fr.mossaab.security.config;

import fr.mossaab.security.service.UserCreateService;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class DemoDataInitializer {

    private final UserCreateService userCreateService;

    @PostConstruct
    @Transactional
    public void init() {
        userCreateService.createUsers();
    }

}
