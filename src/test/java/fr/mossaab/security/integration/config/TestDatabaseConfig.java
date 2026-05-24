package fr.mossaab.security.integration.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
@Profile("test")
public class TestDatabaseConfig {

    @Value("${test.db.type:real}")
    private String testDbType;

    @Autowired
    private Environment env;

    @Bean
    @Primary
    public DataSource dataSource() {
        if ("h2".equalsIgnoreCase(testDbType)) {
            // Используем H2 конфиг из application-test.properties
            return DataSourceBuilder.create()
                    .url(env.getProperty("test.h2.datasource.url"))
                    .username(env.getProperty("test.h2.datasource.username"))
                    .password(env.getProperty("test.h2.datasource.password"))
                    .driverClassName(env.getProperty("test.h2.datasource.driver-class-name"))
                    .build();
        } else {
            // По умолчанию – "real": берём настройки обычного datasource из application.properties
            return DataSourceBuilder.create()
                    .url(env.getProperty("spring.datasource.url"))
                    .username(env.getProperty("spring.datasource.username"))
                    .password(env.getProperty("spring.datasource.password"))
                    .driverClassName(env.getProperty("spring.datasource.driver-class-name"))
                    .build();
        }
    }
}
