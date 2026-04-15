package fr.mossaab.security.exception;

import lombok.Getter;

/**
 * Исключение, выбрасываемое при попытке создать дубликат ресурса (например, пользователя с таким же email или никнеймом).
 */
@Getter
public class DuplicateResourceException extends RuntimeException {

    private final String errorCode;

    public DuplicateResourceException(String message) {
        super(message);
        this.errorCode = "duplicate_resource";
    }

    public DuplicateResourceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "duplicate_resource";
    }

}
