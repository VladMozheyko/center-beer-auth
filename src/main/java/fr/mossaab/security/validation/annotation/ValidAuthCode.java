package fr.mossaab.security.validation.annotation;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.*;

/**
 * Auth code (UUID), используемый для подтверждения.
 * <p>
 * Правила:
 * - Обязателен для заполнения (не может быть null, пустым или состоять только из пробелов).
 * - Должен быть в формате UUID v4:
 * - содержит 36 символов;
 * - включает только шестнадцатеричные цифры (0–9, a–f, A–F) и дефисы.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Schema(
        description = "Auth code, сгенерированный как UUID",
        example = "123e4567-e89b-12d3-a456-426614174000"
)
@NotBlank(message = "Auth code обязателен для заполнения")
@Pattern(
        regexp = "^[0-9a-fA-F\\-]{36}$",
        message = "Некорректный формат auth code"
)
public @interface ValidAuthCode {

    String message() default "Некорректный auth code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
