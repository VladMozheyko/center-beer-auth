package fr.mossaab.security.config;


import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс OpenAPIConfiguration для настройки OpenAPI и Swagger.
 */
@Configuration
public class OpenAPIConfiguration {

    @Value("${app.server.base-url}")
    private String baseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Go Mind API")
                        .version("0.0.1-SNAPSHOT")
                        .description("Документация доступна по ссылке: " + baseUrl + "/v3/api-docs")
                        .contact(new io.swagger.v3.oas.models.info.Contact()
                                .name("Alexandr")
                                .email("kichmarev@list.ru")))
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)));
    }

}
