package fr.mossaab.security.validation.annotation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.*;

/**
 * Валидация refresh token:
 * - Обязателен для заполнения (не пустой).
 * - Должен быть корректной строкой Base64.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Schema(
        description = "Refresh token в формате Base64",
        example = "ZXlKaGJHY2lPaUpJVXpJMU5pSXNJbUZwYzNNaU9pSm9kSFJ3T2k4dllXUnRhVzRnZEdWNGRDSTZJbWgwZEhCek9pOHZkM2QzTG1OdmJRPT0="
)
@NotBlank(message = "Refresh token обязателен для заполнения")
@Pattern(
        // стандартный base64: группы по 4 символа, завершающиеся 0–2 символами '='
        regexp = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$",
        message = "Refresh token должен быть корректной строкой Base64"
)
public @interface ValidRefreshToken {

    String message() default "Некорректный refresh token";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
