package fr.mossaab.security.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.server.base-url:https://api.center.beer/auth_service}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .description("API для аутентификации и авторизации пользователей")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Support")
                                .email("support@center.beer")))
                .servers(List.of(
                        new Server()
                                .url(serverUrl)
                                .description("Production server")
                ));
    }
}
