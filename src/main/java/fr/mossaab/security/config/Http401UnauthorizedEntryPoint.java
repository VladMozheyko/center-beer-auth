package fr.mossaab.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.mossaab.security.handlers.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;


/**
 * Компонент для обработки случаев неавторизованного доступа (401 Unauthorized).
 * Логирует ошибку и возвращает клиенту информацию об ошибке в формате JSON.
 */
@Component
@Slf4j
public class Http401UnauthorizedEntryPoint implements AuthenticationEntryPoint {

    /**
     * Обрабатывает неавторизованный доступ.
     *
     * @param request HTTP запрос.
     * @param response HTTP ответ.
     * @param authException Исключение, указывающее на проблему аутентификации.
     * @throws IOException Если возникает ошибка ввода-вывода.
     * @throws ServletException Если возникает ошибка сервлета.
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        // Логируем ошибку неавторизованного доступа с сообщением исключения.
        log.error("Unauthorized error: {}", authException.getMessage());

        // Устанавливаем тип контента ответа как JSON.
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // Устанавливаем статус ответа как 401 Unauthorized.
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Создаем тело ответа с информацией об ошибке.
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .error("Unauthorized")
                .timestamp(Instant.now())
                .message(authException.getMessage())
                .path(request.getServletPath())
                .build();

        // Создаем объект ObjectMapper для сериализации Java объектов в JSON.
        final ObjectMapper mapper = new ObjectMapper();
        // Регистрируем модуль JavaTimeModule, чтобы Jackson поддерживал типы даты и времени Java 8 и выше.
        mapper.registerModule(new JavaTimeModule());
        // Настраиваем Jackson на сериализацию дат как строк в формате ISO 8601.
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // Записываем тело ответа в выходной поток ответа в формате JSON.
        mapper.writeValue(response.getOutputStream(), body);
    }
}