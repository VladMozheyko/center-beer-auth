package fr.mossaab.security.dto.auth;


import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    /** Код из письма */
    @NotBlank
    private String code;

    /** Новый пароль */
    @NotBlank
    private String newPassword;

    /** Повтор нового пароля */
    @NotBlank
    private String newPasswordRepeat;
}