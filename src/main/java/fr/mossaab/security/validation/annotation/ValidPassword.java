package fr.mossaab.security.validation.annotation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.*;

/**
 * Пароль.
 * <p>
 * Правила:
 * - Обязателен для заполнения (не может быть null, пустым или состоять только из пробелов).
 * - Длина от 5 до 50 символов.
 * - Должен содержать:
 * - хотя бы одну строчную букву (a–z);
 * - хотя бы одну цифру (0–9);
 * - хотя бы один специальный символ из набора: @$!%*?&#.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@NotBlank(message = "Пароль не должен быть пустым.")
@Pattern(
        regexp = "^(?=.*[a-z])(?=.*\\d)[A-Za-z\\d@$!%*?&#]{5,50}$",
        message = "Пароль должен быть длиной от 5 до 50 символов, содержать хотя бы одну строчную букву, одну цифру"
)
public @interface ValidPassword {

    String message() default "Некорректный пароль";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}