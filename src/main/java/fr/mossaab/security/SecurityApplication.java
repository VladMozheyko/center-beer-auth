package fr.mossaab.security;

import fr.mossaab.security.service.impl.BoilerCreateService;
import fr.mossaab.security.service.impl.PassportService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class SecurityApplication {
    private boolean SchemaIsEmpty = false;
    @Autowired
    private BoilerCreateService boilerCreateService;
    @Autowired
    private PassportService passportService;

    public static void main(String[] args) {

        SpringApplication.run(SecurityApplication.class, args);
    }
    @PostConstruct
    public void createSamplePresentation() throws IOException {
        boilerCreateService.CreateBoilers();
        passportService.createAndSavePassportData();
        System.out.println("Пример презентации с файлами успешно создан.");
    }


}
