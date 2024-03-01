package fr.mossaab.security;

import fr.mossaab.security.service.PresentationService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class SecurityApplication {
    @Autowired
    private PresentationService presentationService;

    public static void main(String[] args) {

        SpringApplication.run(SecurityApplication.class, args);
    }
    @PostConstruct
    public void createSamplePresentation() {
        try {
            presentationService.createMultiplePresentations();
            System.out.println("Пример презентации с файлами успешно создан.");
        } catch (IOException e) {
            System.err.println("Ошибка при создании примера презентации: " + e.getMessage());
        }
    }
}
