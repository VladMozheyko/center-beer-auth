package fr.mossaab.security.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Обработка исключения дублирования ресурса (например, email или никнейма).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResourceException(DuplicateResourceException ex) {
        log.warn("[ERROR] - DuplicateResourceException: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", ex.getErrorCode());
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Обработка нарушения целостности данных (дубликат email/nickname в БД).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("[ERROR] - DataIntegrityViolationException: message={}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.CONFLICT.value());

        String message = ex.getMessage();
        if (message != null && message.contains("UK_qrea8hrws39y2x7sxnwiruinh")) {
            body.put("error", "nickname_exists");
            body.put("message", "Пользователь с таким никнеймом уже существует");
        } else if (message != null && message.contains("email")) {
            body.put("error", "email_exists");
            body.put("message", "Пользователь с таким email уже существует");
        } else {
            body.put("error", "data_integrity_violation");
            body.put("message", "Ошибка целостности данных");
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Обработка исключения "Email не подтверждён" и других IllegalStateException.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("[ERROR] - IllegalStateException: message={}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Обработка исключения "Пользователь не найден".
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        log.warn("[ERROR] - UsernameNotFoundException: message={}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Обработка ошибок аутентификации (неверный логин/пароль).
     */
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(Exception ex) {
        log.warn("[ERROR] - AuthenticationException: message={}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", "Неверный email или пароль");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * Ошибки @Validated на @PathVariable / @RequestParam
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("[ERROR] - ConstraintViolationException: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Параметры запроса не прошли валидацию");
        body.put("errors", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Обработка исключения "Пользователь не найден" (RuntimeException).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.warn("[ERROR] - RuntimeException: message={}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.NOT_FOUND.value());

        if ("Пользователь не найден".equals(ex.getMessage())) {
            body.put("error", "token_not_found");
            body.put("message", "Пользователь не найден");
        } else {
            body.put("error", "Internal Server Error");
            body.put("message", ex.getMessage());
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Обработка остальных исключений.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("[ERROR] - Exception: message={}, exception={}", ex.getMessage(), ex.toString(), ex);
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }


    @ExceptionHandler(SocialAuthException.class)
    public ResponseEntity<Map<String, Object>> handleSocialAuthException(SocialAuthException ex) {
        log.warn("[ERROR] - SocialAuthException: status={}, message={}", ex.getHttpStatus(), ex.getMessage());
        int status = ex.getHttpStatus();
        HttpStatus httpStatus = HttpStatus.valueOf(status);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("error", httpStatus.getReasonPhrase());
        body.put("message", ex.getMessage());

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Ошибки @Valid на @RequestBody (DTO)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("[ERROR] - MethodArgumentNotValidException: {}", ex.getMessage());
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> body = new HashMap<>();
        body.put("message", "Данные не прошли валидацию");
        body.put("errors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}