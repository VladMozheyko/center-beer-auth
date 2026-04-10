package fr.mossaab.security.validation.annotation;


import fr.mossaab.security.validation.validators.ValidImageFileValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Файл изображения для загрузки.
 * <p>
 * Правила:
 * - Обязателен (не может быть null).
 * - Не должен быть пустым.
 * - Максимальный размер — 5 МБ.
 * - Допустимые MIME-типы: image/jpeg, image/png.
 */
@Constraint(validatedBy = ValidImageFileValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Schema(
        description = "Файл изображения (JPEG или PNG) размером до 5 МБ",
        example = "avatar.png"
)
public @interface ValidImageFile {

    String message() default "Некорректный файл изображения";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    long maxSizeBytes() default 5 * 1024 * 1024; // 5 МБ

    String[] allowedContentTypes() default {
            "image/jpeg",
            "image/png"
    };
}
