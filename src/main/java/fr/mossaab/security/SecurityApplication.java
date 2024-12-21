package fr.mossaab.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@SpringBootApplication
public class SecurityApplication {
    private boolean SchemaIsEmpty = false;

    public static void main(String[] args) {

        SpringApplication.run(SecurityApplication.class, args);
    }
    @Transactional
    @PostConstruct
    public void createSamplePresentation() throws IOException {
    }


}
