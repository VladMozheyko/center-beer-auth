package fr.mossaab.security.validation.annotation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.*;

/**
 * Телефон, который подтверждаем.
 * <p>
 * Правила:
 * - Обязателен для заполнения (не может быть null, пустым или состоять только из пробелов).
 * - Формат: +7XXXXXXXXXX.
 * - После +7 должно быть ровно 10 цифр (всего 12 символов, включая '+').
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@NotBlank(message = "Телефон обязателен для заполнения")
@Pattern(
        regexp = "^\\+7\\d{10}$",
        message = "Телефон должен быть в формате +7XXXXXXXXXX и содержать 10 цифр после +7"
)
public @interface ValidRussianPhone {

    String message() default "Некорректный телефон";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
