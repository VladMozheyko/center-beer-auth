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
                title = "GoMind API",
                description = """
                        <strong>API для отопительных систем.</strong>
                        
                        В системе котлов сначала создается сущность на основе полей
                        посредством методов create, а позже привязываются связные сущности
                        Посредством методов update соответственно.
                        Для создания объектов, в которых присутствует связная сущность в виде файла
                        тестируется посредством Postman.
                        
                        <strong>Шаги для скачивания OpenAPI спецификации:</strong>
                        1. Перейдите к файлу спецификации OpenAPI:
                            http://31.129.102.70:8081/v3/api-docs
                        2. Загрузка спецификации:
                            Откроется JSON-файл с описанием всех ваших API.
                        3. Чтобы его скачать, щелкните правой кнопкой мыши на странице и выберите "Сохранить как...", затем сохраните файл как .json.
                        4.  Импорт файла в Postman:
                            Откройте Postman и нажмите "Import".
                            Выберите скачанный файл .json.
                            После этого Postman автоматически создаст коллекцию с запросами.
                                               
                        Теперь у вас в Postman будут все запросы API, описанные в спецификации.
                        
                        <strong>Шаги для установки cookie для запросов, где он нужен:</strong>
                        1. Правой кнопкой мыши в браузере нажмите "Посмотреть код"
                           после того как успешно выполните запрос /authentication/login
                        2. Перейдите во вкладу Application
                        3. Потом перейдите внутри Application во вкладку Cookies
                        4. Там должен быть refresh-jwt-cookie скопируете его значение и закройте панель
                           которую открыли в первом пункте
                        5. Нажмите на зеленый значок "Authorize" и в Value вставьте значение refresh-jwt-cookie
                           и нажмите Authorize
                           
                        Теперь вам можно использовать методы требующие авторизацию посредством токенов.
                        """,
                version = "0.0.1-SNAPSHOT"
        ),
        servers = {},  // Placeholder, will be added dynamically
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


    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> openApi.addServersItem(new io.swagger.v3.oas.models.servers.Server().url("http://31.129.102.70:8080"));
    }

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