package fr.mossaab.security.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.*;

/**
 * Имя файла изображения.
 * <p>
 * Правила:
 * - Обязательно для заполнения (не может быть null, пустым или состоять только из пробелов).
 * - Не должно содержать относительных переходов директорий ("../").
 * - Должно иметь одно из расширений: .png, .jpg, .jpeg.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@NotBlank(message = "Имя файла обязательно для заполнения")
@Pattern(
        regexp = "^(?!.*\\.\\.\\/).+\\.(png|jpg|jpeg)$",
        message = "Имя файла не должно содержать относительных переходов директорий и должно иметь расширение .png, .jpg или .jpeg"
)
public @interface ValidImageFileName {

    String message() default "Некорректное имя файла изображения";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
