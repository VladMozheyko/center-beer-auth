package fr.mossaab.security;

import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@SpringBootApplication
public class SecurityApplication {
    private boolean SchemaIsEmpty = false;
    @Autowired
    private BoilerCreateService boilerCreateService;

    public static void main(String[] args) {

        SpringApplication.run(SecurityApplication.class, args);
    }
    @PostConstruct
    public void createSamplePresentation() {
        boilerCreateService.CreateBoilers();
        System.out.println("Пример презентации с файлами успешно создан.");
    }


}
