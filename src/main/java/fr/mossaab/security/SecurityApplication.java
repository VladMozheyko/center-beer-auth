package fr.mossaab.security;

import fr.mossaab.security.service.UserCreateService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class SecurityApplication {

    @Autowired
    private UserCreateService userCreationService;

    public static void main(String[] args) {
        SpringApplication.run(SecurityApplication.class, args);
    }

    @Transactional
    @PostConstruct
    public void createSamplePresentation() throws IOException {
        userCreationService.createUsers();
        printPhysicalResourcesFolder();
    }

    private void printPhysicalResourcesFolder() {
        String resourcePath = "src/main/resources";

        try {
            Files.walk(Paths.get(resourcePath))
                    .filter(Files::isRegularFile)
                    .forEach(path -> System.out.println("📄 " + path.toAbsolutePath()));
        } catch (IOException e) {
            System.err.println("Ошибка при чтении папки: " + e.getMessage());
        }
    }


}
