package fr.mossaab.security.validation.annotation;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.lang.annotation.*;

/**
 * Идентификатор (ID).
 * <p>
 * Правила:
 * - Обязателен для заполнения (не может быть null).
 * - Должен быть положительным числом (минимальное значение — 1).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Schema(
        description = "Идентификатор сущности",
        example = "1"
)
@NotNull(message = "ID обязателен для заполнения")
@Min(value = 1, message = "ID должен быть положительным числом")
public @interface ValidId {

    String message() default "Некорректный идентификатор";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
