package fr.mossaab.security.dto.auth;


import fr.mossaab.security.validation.annotation.ValidEmail;
import fr.mossaab.security.validation.annotation.ValidPassword;
import fr.mossaab.security.validation.annotation.ValidRuEnNicknameLengthMin4Max50;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @ValidRuEnNicknameLengthMin4Max50
    private String nickname;

    /**
     * Электронная почта пользователя.
     */
    @Schema(description = "Почтовый адрес пользователя", example = "example@gmail.ru")
    @ValidEmail
    private String email;

    /**
     * Пароль пользователя.
     */
    @Schema(description = "Пароль пользователя", example = "Sasha123!")
    @ValidPassword
    private String password;

}
