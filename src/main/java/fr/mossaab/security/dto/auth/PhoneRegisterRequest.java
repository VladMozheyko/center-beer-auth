package fr.mossaab.security.dto.auth;

import fr.mossaab.security.enums.RegistrationMethod;
import fr.mossaab.security.validation.annotation.ValidEmail;
import fr.mossaab.security.validation.annotation.ValidPassword;
import fr.mossaab.security.validation.annotation.ValidRuEnNicknameLengthMin4Max50;
import fr.mossaab.security.validation.annotation.ValidRussianPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PhoneRegisterRequest {

    @Schema(description = "Никнейм пользователя", example = "АмурскийТигр1995")
    @ValidRuEnNicknameLengthMin4Max50
    private String nickname;

    @Schema(description = "Электронная почта пользователя", example = "example@gmail.com")
    @ValidEmail
    private String email;

    @Schema(description = "Пароль пользователя", example = "Sasha123!")
    @ValidPassword
    private String password;

    @Schema(description = "Номер телефона пользователя", example = "+79955941557")
    @ValidRussianPhone
    private String phone;

    @Schema(description = "Метод подтверждения регистрации (CALL — звонок, SMS — SMS-сообщение)", example = "CALL")
    @NotNull(message = "Метод подтверждения регистрации обязателен для заполнения")
    private RegistrationMethod method;
}