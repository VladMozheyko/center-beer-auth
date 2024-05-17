package fr.mossaab.security.config;


import fr.mossaab.security.handlers.ErrorResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс OpenAPIConfiguration для настройки OpenAPI и Swagger.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "Alexandr",
                        email = "kichmarev@list.ru"
                ),
                title = "Spring Security 6+ APP",
                description = "Пример Spring Boot 3+ Spring Security 6+",
                version = "0.0.1-SNAPSHOT"
        ),
        servers = {
                @Server(
                        description = "Development",
                        url = "http://31.129.102.70:8080"
                )
        },
        security = {
                @SecurityRequirement(
                        name = "bearerAuth"
                )
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Описание аутентификации JWT",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenAPIConfiguration {

        /**
         * Создает OpenApiCustomizer для настройки схемы модели ErrorResponse.
         *
         * @return OpenApiCustomizer для настройки схемы модели ErrorResponse
         */
        @Bean
        public OpenApiCustomizer schemaCustomizer() {
                // Разрешение схемы модели ErrorResponse
                ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                        .resolveAsResolvedSchema(new AnnotatedType(ErrorResponse.class));
                // Создание и настройка OpenApiCustomizer для установки схемы модели ErrorResponse
                return openApi -> openApi
                        .schema(resolvedSchema.schema.getName(), resolvedSchema.schema);
        }
}
