package fr.mossaab.security.payload.request;

import fr.mossaab.security.enums.Role;
import fr.mossaab.security.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на регистрацию нового пользователя.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /**
     * Имя пользователя.
     */
    //@NotBlank(message = "firstname is required")
    private String firstname;

    /**
     * Фамилия пользователя.
     */
    //@NotBlank(message = "lastname is required")
    private String lastname;

    /**
     * Электронная почта пользователя.
     */
    //@NotBlank(message = "email is required")
    //@Email(message = "email format is not valid")
    private String email;

    /**
     * Пароль пользователя.
     */
    //@NotBlank(message = "password is required")
    //@StrongPassword
    private String password;

    /**
     * Роль пользователя.
     */
    //@NotNull
    private String workerRole;

    /**
     * Дата рождения пользователя в формате строки.
     */
    private String dateOfBirth;

    /**
     * Номер телефона пользователя.
     */
    private String phoneNumber;

}
