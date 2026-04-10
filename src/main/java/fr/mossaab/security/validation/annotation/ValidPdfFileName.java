package fr.mossaab.security.validation.annotation;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.*;

/**
 * Имя PDF-файла.
 * <p>
 * Правила:
 * - Обязательно для заполнения (не может быть null, пустым или состоять только из пробелов).
 * - Должно оканчиваться на ".pdf".
 * - Не должно содержать относительных переходов директорий ("../").
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Schema(
        description = "Имя PDF-файла для загрузки из файловой системы",
        example = "document_2024.pdf"
)
@NotBlank(message = "Имя файла обязательно для заполнения")
@Pattern(
        regexp = "^(?!.*\\.\\.\\/).+\\.pdf$",
        message = "Имя файла должно оканчиваться на .pdf и не должно содержать относительных переходов директорий"
)
public @interface ValidPdfFileName {

    String message() default "Некорректное имя PDF-файла";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
