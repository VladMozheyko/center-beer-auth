package fr.mossaab.security;

import fr.mossaab.security.service.impl.*;
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
    @Autowired
    private DocumentService documentService;
    @Autowired
    private CreateServiceCentreService createServiceCentreService;
    @Autowired
    private StorageService storageService;

    public static void main(String[] args) {

        SpringApplication.run(SecurityApplication.class, args);
    }
    @PostConstruct
    public void createSamplePresentation() throws IOException {
        boilerCreateService.CreateBoilers();
        passportService.createAndSavePassportData();
        documentService.createAndSaveDocumentData();
        createServiceCentreService.CreateServiceCentre();
        storageService.uploadImageToFileSystemDefaultAvatar();
        System.out.println("Пример презентации с файлами успешно создан.");
    }


}
