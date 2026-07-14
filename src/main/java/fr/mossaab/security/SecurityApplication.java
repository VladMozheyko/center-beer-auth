package fr.mossaab.security;

import fr.mossaab.security.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
public class SecurityApplication {

    public static void main(String[] args) {
        DotenvLoader.load(); //загрузка переменных их .env
        SpringApplication.run(SecurityApplication.class, args);
    }
}