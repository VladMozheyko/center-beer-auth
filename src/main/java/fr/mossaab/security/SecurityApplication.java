package fr.mossaab.security;

import fr.mossaab.security.service.UserCreateService;
import fr.mossaab.security.controller.QuizController;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class SecurityApplication {
    private boolean schemaIsEmpty = false;

    @Autowired
    private UserCreateService userCreationService;

    // Внедрение QuizController для вызова метода обновления вопросов при запуске
    @Autowired
    private QuizController quizController;

    public static void main(String[] args) {
        SpringApplication.run(SecurityApplication.class, args);
    }

    @Transactional
    @PostConstruct
    public void createSamplePresentation() throws IOException {
        userCreationService.createUsers();
        System.out.println("Пример презентации с файлами успешно создан.");

        // Автоматическая подгрузка вопросов при запуске
        String result = quizController.updateQuestionsFromCSV();
        System.out.println("Автоматическое обновление вопросов: " + result);
    }
}
