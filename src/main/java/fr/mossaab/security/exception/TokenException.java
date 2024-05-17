package fr.mossaab.security.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, выбрасываемое при проблемах с токеном.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class TokenException extends RuntimeException {

    /**
     * Создает исключение с заданным токеном и сообщением.
     *
     * @param token   Токен, вызвавший исключение.
     * @param message Сообщение об ошибке.
     */
    public TokenException(String token, String message) {
        super(String.format("Не удалось для [%s]: %s", token, message));
    }

}
