package fr.mossaab.security.entities;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ApiResponse<T> {
    private LocalDateTime timestamp;    // Время выполнения
    private int status;                 // HTTP-статус
    private String message;             // Сообщение о результате
    private String path;                // URI маршрута
    private String method;              // HTTP-метод
    private String processingTime;      // Время обработки запроса
    private String user;                // Имя пользователя или ID
    private T data;                     // Основная нагрузка данных
    private List<String> errors;        // Список ошибок (если есть)
    private Integer count;
}