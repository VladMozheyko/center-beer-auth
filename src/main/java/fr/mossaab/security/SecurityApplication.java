package fr.mossaab.security;

import fr.mossaab.security.service.UserCreateService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@SpringBootApplication
@EnableScheduling
public class SecurityApplication {
    private boolean SchemaIsEmpty = false;
    @Autowired
    private UserCreateService userCreationService;

    public static void main(String[] args) {

        SpringApplication.run(SecurityApplication.class, args);
    }
    @Transactional
    @PostConstruct
    public void createSamplePresentation() throws IOException {
        userCreationService.createUsers();
        System.out.println("Пример презентации с файлами успешно создан.");
    }


}
