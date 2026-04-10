package fr.mossaab.security.dto.auth;


import fr.mossaab.security.validation.annotation.ValidPassword;
import fr.mossaab.security.validation.annotation.ValidSmsCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    /** Код из письма */
    @Schema(example = "7072", description = "Код из звонка/СМС")
    @ValidSmsCode
    private String code;

    /** Новый пароль */
    @Schema(example = "Az0Za91", description = "Новый пароль")
    @ValidPassword
    private String newPassword;

    /** Повтор нового пароля */
    @Schema(example = "Az0Za91", description = "Повтор нового пароля")
    @ValidPassword
    private String newPasswordRepeat;
}