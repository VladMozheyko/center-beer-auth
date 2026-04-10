package fr.mossaab.security.validation.annotation;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.*;

/**
 * Никнейм пользователя.
 * <p>
 * Правила:
 * - Обязателен для заполнения (не может быть null, пустым или состоять только из пробелов).
 * - Длина от 4 до 50 символов.
 * - Может содержать:
 * - буквы латиницы (A–Z, a–z);
 * - буквы кириллицы (А–Я, а–я, Ё, ё);
 * - цифры (0–9);
 * - символы '_' и '!'.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Schema(description = "Никнейм пользователя", example = "АмурскийТигр1995")
@NotBlank(message = "Никнейм обязателен для заполнения")
@Pattern(
        regexp = "^[A-Za-zА-Яа-яЁё0-9_!]{4,50}$",
        message = "Никнейм должен быть длиной от 4 до 50 символов и может содержать буквы (латиница и кириллица), цифры, а также символы _ и !"
)
public @interface ValidRuEnNicknameLengthMin4Max50 {

    String message() default "Некорректный никнейм";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
