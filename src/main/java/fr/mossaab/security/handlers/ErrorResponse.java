package fr.mossaab.security.handlers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Класс для представления объекта ответа об ошибке.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    /**
     * Статус ошибки.
     */
    private int status;

    /**
     * Ошибка.
     */
    private String error;

    /**
     * Временная метка.
     */
    private Instant timestamp;

    /**
     * Сообщение об ошибке.
     */
    private String message;

    /**
     * Путь, вызвавший ошибку.
     */
    private String path;
}
