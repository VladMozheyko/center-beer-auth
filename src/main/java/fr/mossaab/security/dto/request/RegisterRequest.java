package fr.mossaab.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "Имя пользователя", example = "Александр")
    private String firstname;

    /**
     * Фамилия пользователя.
     */
    @Schema(description = "Фамилия пользователя", example = "Иванов")
    private String lastname;

    /**
     * Электронная почта пользователя.
     */
    @Schema(description = "Почтовый адрес пользователя", example = "example@gmail.ru")
    private String email;

    /**
     * Пароль пользователя.
     */
    @Schema(description = "Пароль пользователя", example = "Sasha123!")
    private String password;

    /**
     * Роль пользователя.
     */
    @Schema(description = "Вид контрагента", example = "RETAIL_CUSTOMER")
    private String workerRole;

    /**
     * Дата рождения пользователя в формате строки.
     */
    @Schema(description = "Дата рождения", example = "2000-01-01")
    private String dateOfBirth;

    /**
     * Номер телефона пользователя.
     */
    @Schema(description = "Номер телефона", example = "+78005553555")
    private String phoneNumber;

}
