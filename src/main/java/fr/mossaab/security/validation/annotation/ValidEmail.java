package fr.mossaab.security.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER}) // Где будет применяться аннотация (поле, параметр)
@Retention(RetentionPolicy.RUNTIME) // Аннотация будет доступна во время выполнения
@Constraint(validatedBy = {}) // Важно: указываем пустой массив, так как это составная аннотация
@NotBlank(message = "Email обязательно для заполнения")
@Email(message = "Некорректный email")
@Size(max = 100, message = "Email не должен превышать 100 символов")
public @interface ValidEmail {

    // Стандартные методы для аннотаций валидации
    String message() default "Email обязательно для заполнения"; // Сообщение по умолчанию, если не переопределено

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
