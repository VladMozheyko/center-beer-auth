package fr.mossaab.security;

import fr.mossaab.security.service.UserCreateService;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class SecurityApplication {

    @Autowired
    private UserCreateService userCreationService;

    /*
     * Spring сам читает .env (библиотеку dotenv-java)
     * Теперь просто кладем .env в корень проекта,
     *  и Spring подтянет значения (доступны как ${VK_CLIENT_ID} и т.п.).
     */
    static {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> {
            String key = entry.getKey();
            String value = entry.getValue();

            System.setProperty(key, value);
        });
    }

    public static void main(String[] args) {
        SpringApplication.run(SecurityApplication.class, args);
    }

    @Transactional
    @PostConstruct
    public void createSamplePresentation()  {
        userCreationService.createUsers();
    }

}