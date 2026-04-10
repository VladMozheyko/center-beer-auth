package fr.mossaab.security.validation.annotation;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.*;

/**
 * Код подтверждения (из звонка/СМС).
 * <p>
 * Правила:
 * - Обязателен для заполнения (не может быть null, пустым или состоять только из пробелов).
 * - Должен состоять ровно из 4 символов.
 * - Все символы — только цифры (0–9).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@NotBlank(message = "Код обязателен для заполнения")
@Pattern(
        regexp = "^\\d{4}$",
        message = "Код должен состоять из 4 цифр"
)
public @interface ValidSmsCode {

    String message() default "Некорректный код";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
