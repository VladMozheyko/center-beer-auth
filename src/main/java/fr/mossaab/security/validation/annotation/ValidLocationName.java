package fr.mossaab.security.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER}) // Где будет применяться аннотация (поле, параметр)
@Retention(RetentionPolicy.RUNTIME) // Аннотация будет доступна во время выполнения
@Constraint(validatedBy = {}) // Важно: указываем пустой массив, так как это составная аннотация
@Size(max = 100, message = "Название страны не должно превышать 100 символов")
@Pattern(
        regexp = "^[\\p{L} .\\-]*$",
        message = "Название страны может содержать только буквы, пробелы, точки и дефисы"
)
public @interface ValidLocationName {

    // Стандартные методы для аннотаций валидации
    String message() default "Невалидное название страны";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
