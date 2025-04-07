package fr.mossaab.security.dto.auth;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /**
     * Фамилия пользователя.
     */
    @Schema(description = "Никнейм пользователя", example = "АмурскийТигр1995")
    private String nickname;

    /**
     * Электронная почта пользователя.
     */

    @Schema(description = "Почтовый адрес пользователя", example = "example@gmail.ru")
    private String email;

    /**
     * Пароль пользователя.
     */
    @NotBlank(message = "Пароль не должен быть пустой.")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{7,50}$",
            message = "Пароль должен быть длиной от 8 до 50 символов, содержать хотя бы одну заглавную букву, одну строчную букву, одну цифру и один специальный символ."
    )
    @Schema(description = "Пароль пользователя", example = "Sasha123!")
    private String password;

}
